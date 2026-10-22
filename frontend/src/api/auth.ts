import { requisitar } from './cliente'
import type {
  CadastroRequest,
  ConfiguracaoCadastro,
  LoginRequest,
  LoginResponse,
} from './tipos'
import { definirToken } from './token'

// O refresh token chega no Set-Cookie (httpOnly); só o access token vem no
// corpo e fica em memória.
export async function entrar(credenciais: LoginRequest): Promise<void> {
  const { tokenAcesso } = await requisitar<LoginResponse>('/auth/login', {
    metodo: 'POST',
    corpo: credenciais,
  })
  definirToken(tokenAcesso)
}

export function sair(): Promise<void> {
  return requisitar<void>('/auth/logout', { metodo: 'POST' })
}

// O cadastro está aberto? Pede código de convite? A tela decide o que mostrar.
export function carregarConfiguracaoCadastro(
  sinal?: AbortSignal,
): Promise<ConfiguracaoCadastro> {
  return requisitar<ConfiguracaoCadastro>('/auth/registro', { sinal })
}

// Só cria a conta (201): quem usa entra em seguida com as mesmas credenciais.
export async function registrar(corpo: CadastroRequest): Promise<void> {
  await requisitar<unknown>('/auth/registrar', { metodo: 'POST', corpo })
}
