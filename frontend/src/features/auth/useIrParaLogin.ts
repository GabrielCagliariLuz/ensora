import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { limparToken } from '../../api/token'
import { CAMINHOS } from '../../lib/caminhos'
import type { AvisoLogin } from './avisos'

// Encerra a sessão só no aparelho (token em memória e cache) e leva ao login.
// Serve a quem já perdeu a sessão no servidor: sair, trocar a senha, excluir a
// conta. O aviso, se houver, aparece uma vez na tela de login.
export function useIrParaLogin() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return (aviso?: AvisoLogin) => {
    limparToken()
    queryClient.clear()
    void navigate(CAMINHOS.login, {
      replace: true,
      state: aviso ? { aviso } : undefined,
    })
  }
}
