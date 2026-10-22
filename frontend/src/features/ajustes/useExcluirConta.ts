import { useMutation } from '@tanstack/react-query'
import { excluirConta } from '../../api/conta'
import { useIrParaLogin } from '../auth/useIrParaLogin'

// Apaga a conta e tudo o que é dela no servidor; o aparelho só esquece a sessão.
export function useExcluirConta() {
  const irParaLogin = useIrParaLogin()

  return useMutation({
    mutationFn: excluirConta,
    onSuccess: () => irParaLogin('conta-excluida'),
  })
}
