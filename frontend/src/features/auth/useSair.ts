import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { sair } from '../../api/auth'
import { limparToken } from '../../api/token'
import { CAMINHOS } from '../../lib/caminhos'

export function useSair() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: sair,
    // onSettled roda com sucesso ou falha: o estado local é apagado mesmo se
    // a revogação no servidor não chegar (rede fora, por exemplo).
    onSettled: () => {
      limparToken()
      queryClient.clear()
      void navigate(CAMINHOS.login, { replace: true })
    },
  })
}
