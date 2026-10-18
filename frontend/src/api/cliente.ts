import { ErroApi } from './erro'
import type { ErroResposta, LoginResponse } from './tipos'
import { definirToken, limparToken, obterToken } from './token'

const URL_BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface OpcoesRequisicao {
  metodo?: Metodo
  corpo?: unknown
  sinal?: AbortSignal
}

let aoSessaoExpirar: (() => void) | null = null

// A #11 registra aqui, em app/, a limpeza do cache e o redirecionamento
// para /login. O cliente não conhece o router.
export function definirAoSessaoExpirar(funcao: (() => void) | null): void {
  aoSessaoExpirar = funcao
}

// Rotas /auth/* nunca recebem Authorization: com token inválido o Spring
// responde 401 até nas rotas públicas. Também não disparam renovação.
function ehRotaAuth(caminho: string): boolean {
  return caminho.startsWith('/auth/')
}

async function enviar(
  caminho: string,
  opcoes: OpcoesRequisicao,
  token: string | null,
): Promise<Response> {
  const cabecalhos: Record<string, string> = {}
  if (opcoes.corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json'
  }
  if (token !== null && !ehRotaAuth(caminho)) {
    cabecalhos.Authorization = `Bearer ${token}`
  }

  try {
    return await fetch(`${URL_BASE}${caminho}`, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
      credentials: 'include',
      signal: opcoes.sinal,
    })
  } catch (erro) {
    if (erro instanceof DOMException && erro.name === 'AbortError') throw erro
    throw new ErroApi(0, 'SEM_CONEXAO', 'Sem conexão com o servidor')
  }
}

async function lerErro(resposta: Response): Promise<ErroApi> {
  try {
    const corpo = (await resposta.json()) as Partial<ErroResposta>
    return new ErroApi(
      resposta.status,
      corpo.erro ?? 'ERRO_DESCONHECIDO',
      corpo.mensagem ?? resposta.statusText,
      corpo.campos,
    )
  } catch {
    return new ErroApi(
      resposta.status,
      'ERRO_DESCONHECIDO',
      resposta.statusText || 'Erro inesperado',
    )
  }
}

async function executarRenovacao(): Promise<string> {
  const resposta = await enviar('/auth/refresh', { metodo: 'POST' }, null)
  if (!resposta.ok) throw await lerErro(resposta)

  const { tokenAcesso } = (await resposta.json()) as LoginResponse
  definirToken(tokenAcesso)
  return tokenAcesso
}

let renovacaoEmAndamento: Promise<string> | null = null

// O refresh token é rotativo: um reuso revoga todas as sessões. Por isso só
// existe uma renovação por vez, e quem chega durante ela espera a mesma.
export function renovarSessao(): Promise<string> {
  renovacaoEmAndamento ??= executarRenovacao().finally(() => {
    renovacaoEmAndamento = null
  })
  return renovacaoEmAndamento
}

async function renovarAposNaoAutorizado(
  tokenEnviado: string | null,
): Promise<string> {
  // Outra requisição já renovou enquanto esta estava em voo: usa o token novo.
  const atual = obterToken()
  if (atual !== null && atual !== tokenEnviado) return atual

  try {
    return await renovarSessao()
  } catch (erro) {
    // Só um 401 do refresh significa sessão perdida. Falha de rede ou 5xx
    // mantém o token e deixa a requisição falhar normalmente. O callback
    // dispara uma vez: a primeira requisição limpa o token, as demais já o
    // encontram vazio.
    if (erro instanceof ErroApi && erro.status === 401 && obterToken() !== null) {
      limparToken()
      aoSessaoExpirar?.()
    }
    throw erro
  }
}

export async function requisitar<T>(
  caminho: string,
  opcoes: OpcoesRequisicao = {},
): Promise<T> {
  const tokenEnviado = obterToken()
  let resposta = await enviar(caminho, opcoes, tokenEnviado)

  if (resposta.status === 401 && !ehRotaAuth(caminho)) {
    const tokenNovo = await renovarAposNaoAutorizado(tokenEnviado)
    resposta = await enviar(caminho, opcoes, tokenNovo)
  }

  if (!resposta.ok) throw await lerErro(resposta)
  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}
