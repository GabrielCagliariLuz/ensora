import { useMutation } from '@tanstack/react-query'
import { sair } from '../../api/auth'
import { useIrParaLogin } from './useIrParaLogin'

export function useSair() {
  const irParaLogin = useIrParaLogin()

  return useMutation({
    mutationFn: sair,
    // onSettled roda com sucesso ou falha: o estado local é apagado mesmo se
    // a revogação no servidor não chegar (rede fora, por exemplo).
    onSettled: () => irParaLogin(),
  })
}
