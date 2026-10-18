import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { definirAoSessaoExpirar, requisitar } from './cliente'
import { ErroApi } from './erro'
import { definirToken, limparToken, obterToken } from './token'

const fetchMock = vi.fn<typeof fetch>()
const aoExpirar = vi.fn()

function json(status: number, corpo?: unknown): Response {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function sessao(tokenAcesso: string): Response {
  return json(200, { tokenAcesso, expiraEmSegundos: 900 })
}

function chamada(indice: number) {
  const [url, init] = fetchMock.mock.calls[indice]
  return {
    url: String(url),
    init: init ?? {},
    cabecalhos: (init?.headers ?? {}) as Record<string, string>,
  }
}

function chamadasPara(caminho: string) {
  return fetchMock.mock.calls.filter(([url]) => String(url).endsWith(caminho))
}

async function falha(promessa: Promise<unknown>): Promise<unknown> {
  return promessa.then(
    () => {
      throw new Error('a requisição deveria ter falhado')
    },
    (erro: unknown) => erro,
  )
}

beforeEach(() => {
  fetchMock.mockReset()
  aoExpirar.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  limparToken()
  definirAoSessaoExpirar(aoExpirar)
})

afterEach(() => {
  vi.unstubAllGlobals()
  limparToken()
  definirAoSessaoExpirar(null)
})

describe('requisição', () => {
  it('envia Authorization e credentials include', async () => {
    definirToken('t1')
    fetchMock.mockResolvedValueOnce(json(200, { ok: true }))

    const resultado = await requisitar<{ ok: boolean }>('/habitos')

    expect(resultado).toEqual({ ok: true })
    const { url, init, cabecalhos } = chamada(0)
    expect(url).toMatch(/\/habitos$/)
    expect(init.method).toBe('GET')
    expect(init.credentials).toBe('include')
    expect(cabecalhos.Authorization).toBe('Bearer t1')
  })

  it('não envia Authorization em /auth/*', async () => {
    definirToken('t1')
    fetchMock.mockResolvedValueOnce(sessao('t2'))

    await requisitar('/auth/login', {
      metodo: 'POST',
      corpo: { email: 'a@b.c', senha: '1234' },
    })

    const { init, cabecalhos } = chamada(0)
    expect(cabecalhos.Authorization).toBeUndefined()
    expect(cabecalhos['Content-Type']).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ email: 'a@b.c', senha: '1234' }))
  })

  it('devolve undefined em 204', async () => {
    fetchMock.mockResolvedValueOnce(json(204))

    await expect(requisitar('/habitos/1', { metodo: 'DELETE' })).resolves
      .toBeUndefined()
  })

  it('traz os campos inválidos no erro 4xx', async () => {
    const campos = [{ campo: 'nome', mensagem: 'não pode estar em branco' }]
    fetchMock.mockResolvedValueOnce(
      json(400, {
        status: 400,
        erro: 'VALIDACAO',
        mensagem: 'Dados inválidos',
        caminho: '/habitos',
        campos,
      }),
    )

    const erro = await falha(requisitar('/habitos', { metodo: 'POST', corpo: {} }))

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({
      status: 400,
      erro: 'VALIDACAO',
      message: 'Dados inválidos',
      campos,
    })
  })

  it('transforma falha de rede em ErroApi com status 0', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    const erro = await falha(requisitar('/habitos'))

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({ status: 0, erro: 'SEM_CONEXAO' })
  })

  it('não mascara o cancelamento da requisição', async () => {
    fetchMock.mockRejectedValueOnce(new DOMException('cancelada', 'AbortError'))

    const erro = await falha(requisitar('/habitos'))

    expect(erro).not.toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({ name: 'AbortError' })
  })
})

describe('renovação de sessão após 401', () => {
  it('renova uma vez e repete a requisição com o novo token', async () => {
    definirToken('velho')
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(sessao('novo'))
      .mockResolvedValueOnce(json(200, { ok: true }))

    const resultado = await requisitar('/habitos')

    expect(resultado).toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    const refresh = chamada(1)
    expect(refresh.url).toMatch(/\/auth\/refresh$/)
    expect(refresh.init.method).toBe('POST')
    expect(refresh.init.credentials).toBe('include')
    expect(refresh.cabecalhos.Authorization).toBeUndefined()
    expect(chamada(2).cabecalhos.Authorization).toBe('Bearer novo')
    expect(obterToken()).toBe('novo')
    expect(aoExpirar).not.toHaveBeenCalled()
  })

  it('não entra em laço se a repetição também responde 401', async () => {
    definirToken('velho')
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(sessao('novo'))
      .mockResolvedValueOnce(json(401))

    const erro = await falha(requisitar('/habitos'))

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({ status: 401 })
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('com a renovação recusada, limpa o token e avisa a sessão expirada', async () => {
    definirToken('velho')
    fetchMock.mockResolvedValueOnce(json(401)).mockResolvedValueOnce(
      json(401, {
        status: 401,
        erro: 'NAO_AUTENTICADO',
        mensagem: 'Sessão inválida',
        caminho: '/auth/refresh',
      }),
    )

    const erro = await falha(requisitar('/habitos'))

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({ status: 401, message: 'Sessão inválida' })
    expect(obterToken()).toBeNull()
    expect(aoExpirar).toHaveBeenCalledTimes(1)
  })

  it('falha de rede na renovação não derruba a sessão', async () => {
    definirToken('velho')
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))

    const erro = await falha(requisitar('/habitos'))

    expect(erro).toMatchObject({ status: 0, erro: 'SEM_CONEXAO' })
    expect(obterToken()).toBe('velho')
    expect(aoExpirar).not.toHaveBeenCalled()
  })

  it('duas requisições 401 simultâneas geram uma só renovação', async () => {
    definirToken('velho')
    fetchMock.mockImplementation(async (url, init) => {
      const caminho = String(url)
      if (caminho.endsWith('/auth/refresh')) {
        await new Promise((resolver) => setTimeout(resolver, 10))
        return sessao('novo')
      }
      const cabecalhos = (init?.headers ?? {}) as Record<string, string>
      return cabecalhos.Authorization === 'Bearer novo'
        ? json(200, { caminho })
        : json(401)
    })

    const [a, b] = await Promise.all([
      requisitar<{ caminho: string }>('/a'),
      requisitar<{ caminho: string }>('/b'),
    ])

    expect(a.caminho).toMatch(/\/a$/)
    expect(b.caminho).toMatch(/\/b$/)
    expect(chamadasPara('/auth/refresh')).toHaveLength(1)
    expect(aoExpirar).not.toHaveBeenCalled()
  })

  it('usa o token novo sem renovar quando outra requisição já renovou', async () => {
    definirToken('velho')
    fetchMock
      .mockImplementationOnce(async () => {
        definirToken('novo')
        return json(401)
      })
      .mockResolvedValueOnce(json(200, { ok: true }))

    await requisitar('/habitos')

    expect(chamadasPara('/auth/refresh')).toHaveLength(0)
    expect(chamada(1).cabecalhos.Authorization).toBe('Bearer novo')
  })

  it('sem token em memória, renova pelo cookie e repete a requisição', async () => {
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(sessao('novo'))
      .mockResolvedValueOnce(json(200, { ok: true }))

    await requisitar('/habitos')

    expect(chamada(0).cabecalhos.Authorization).toBeUndefined()
    expect(chamada(2).cabecalhos.Authorization).toBe('Bearer novo')
    expect(aoExpirar).not.toHaveBeenCalled()
  })

  it('sem sessão nenhuma, falha com 401 sem avisar sessão expirada', async () => {
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(json(401))

    const erro = await falha(requisitar('/habitos'))

    expect(erro).toMatchObject({ status: 401 })
    expect(aoExpirar).not.toHaveBeenCalled()
  })

  it('não renova quando um 401 vem de /auth/*', async () => {
    fetchMock.mockResolvedValueOnce(
      json(401, {
        status: 401,
        erro: 'NAO_AUTENTICADO',
        mensagem: 'E-mail ou senha inválidos',
        caminho: '/auth/login',
      }),
    )

    const erro = await falha(
      requisitar('/auth/login', {
        metodo: 'POST',
        corpo: { email: 'a@b.c', senha: 'errada' },
      }),
    )

    expect(erro).toMatchObject({ status: 401, message: 'E-mail ou senha inválidos' })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
