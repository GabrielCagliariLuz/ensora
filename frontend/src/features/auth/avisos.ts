// Por que a pessoa voltou ao login sem ter clicado em "Sair". Vai no estado da
// navegação, para o login mostrar o aviso uma vez.
export type AvisoLogin = 'senha-alterada' | 'conta-excluida'

export function textoDoAviso(aviso: AvisoLogin): string {
  if (aviso === 'senha-alterada') {
    return 'Senha alterada. Entre de novo com a nova senha.'
  }
  return 'Conta excluída. Seus dados foram apagados.'
}

// O estado da navegação é desconhecido: só aceita os avisos que existem.
export function avisoDoEstado(estado: unknown): AvisoLogin | null {
  const aviso = (estado as { aviso?: unknown } | null)?.aviso
  return aviso === 'senha-alterada' || aviso === 'conta-excluida' ? aviso : null
}
