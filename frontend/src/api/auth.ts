import { requisitar } from './cliente'
import type { LoginRequest, LoginResponse } from './tipos'
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
