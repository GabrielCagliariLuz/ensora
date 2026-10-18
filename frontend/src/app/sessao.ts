import { redirect } from 'react-router'
import { renovarSessao } from '../api/cliente'
import { ErroApi } from '../api/erro'
import { obterToken } from '../api/token'
import { CAMINHOS } from '../lib/caminhos'

// O token só vive em memória, então ao abrir ou recarregar o app ele está
// vazio. O cookie de refresh (httpOnly, 30 dias, renovado a cada uso) é o que
// mantém a pessoa logada: um /auth/refresh devolve um token novo.
// Só 401 significa "sem sessão". Falha de rede ou 5xx sobe para o
// errorElement, que mostra a tela de erro com "Tentar de novo".
async function temSessao(): Promise<boolean> {
  if (obterToken() !== null) return true

  try {
    await renovarSessao()
    return true
  } catch (erro) {
    if (erro instanceof ErroApi && erro.status === 401) return false
    throw erro
  }
}

export async function carregarSessao(): Promise<null> {
  if (!(await temSessao())) throw redirect(CAMINHOS.login)
  return null
}

export async function carregarLogin(): Promise<null> {
  if (await temSessao()) throw redirect(CAMINHOS.inicio)
  return null
}
