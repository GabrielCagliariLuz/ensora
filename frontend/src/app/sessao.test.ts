import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renovarSessao } from '../api/cliente'
import { ErroApi } from '../api/erro'
import { definirToken, limparToken } from '../api/token'
import { carregarLogin, carregarSessao } from './sessao'

vi.mock('../api/cliente', () => ({ renovarSessao: vi.fn() }))

const renovarMock = vi.mocked(renovarSessao)

async function falha(promessa: Promise<unknown>): Promise<unknown> {
  return promessa.then(
    () => {
      throw new Error('o loader deveria ter lançado')
    },
    (erro: unknown) => erro,
  )
}

function destinoDoRedirect(erro: unknown): string | null {
  expect(erro).toBeInstanceOf(Response)
  const resposta = erro as Response
  expect(resposta.status).toBe(302)
  return resposta.headers.get('Location')
}

describe('carregarSessao', () => {
  beforeEach(() => {
    renovarMock.mockReset()
  })

  afterEach(() => {
    limparToken()
  })

  it('com token em memória, segue sem chamar a API', async () => {
    definirToken('abc')

    await expect(carregarSessao()).resolves.toBeNull()
    expect(renovarMock).not.toHaveBeenCalled()
  })

  it('sem token, renova a sessão pelo cookie e segue', async () => {
    renovarMock.mockResolvedValue('novo')

    await expect(carregarSessao()).resolves.toBeNull()
    expect(renovarMock).toHaveBeenCalledTimes(1)
  })

  it('refresh recusado (401) redireciona para o login', async () => {
    renovarMock.mockRejectedValue(
      new ErroApi(401, 'NAO_AUTENTICADO', 'Sessão inválida'),
    )

    const erro = await falha(carregarSessao())

    expect(destinoDoRedirect(erro)).toBe('/login')
  })

  it('falha de rede sobe o erro em vez de mandar para o login', async () => {
    const semConexao = new ErroApi(0, 'SEM_CONEXAO', 'Sem conexão com o servidor')
    renovarMock.mockRejectedValue(semConexao)

    await expect(carregarSessao()).rejects.toBe(semConexao)
  })

  it('erro 5xx sobe o erro em vez de mandar para o login', async () => {
    const interno = new ErroApi(500, 'ERRO_INTERNO', 'Erro interno')
    renovarMock.mockRejectedValue(interno)

    await expect(carregarSessao()).rejects.toBe(interno)
  })
})

describe('carregarLogin', () => {
  beforeEach(() => {
    renovarMock.mockReset()
  })

  afterEach(() => {
    limparToken()
  })

  it('com sessão válida, redireciona para o início', async () => {
    renovarMock.mockResolvedValue('novo')

    const erro = await falha(carregarLogin())

    expect(destinoDoRedirect(erro)).toBe('/')
  })

  it('sem sessão (401), mostra o login', async () => {
    renovarMock.mockRejectedValue(
      new ErroApi(401, 'NAO_AUTENTICADO', 'Sessão inválida'),
    )

    await expect(carregarLogin()).resolves.toBeNull()
  })
})
