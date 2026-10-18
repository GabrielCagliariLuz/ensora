// O access token vive só nesta variável de módulo: nunca em localStorage
// nem sessionStorage. Recarregar a página o apaga; a sessão é recuperada
// por /auth/refresh (cookie httpOnly).
let tokenAcesso: string | null = null

export function obterToken(): string | null {
  return tokenAcesso
}

export function definirToken(token: string): void {
  tokenAcesso = token
}

export function limparToken(): void {
  tokenAcesso = null
}
