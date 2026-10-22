import { useMutation } from '@tanstack/react-query'
import { alterarSenha } from '../../api/conta'
import { useIrParaLogin } from '../auth/useIrParaLogin'

interface TrocaDeSenha {
  senhaAtual: string
  novaSenha: string
}

// O servidor encerra todas as sessões ao trocar a senha, inclusive esta:
// a pessoa volta ao login e entra com a senha nova.
export function useAlterarSenha() {
  const irParaLogin = useIrParaLogin()

  return useMutation({
    mutationFn: ({ senhaAtual, novaSenha }: TrocaDeSenha) =>
      alterarSenha(senhaAtual, novaSenha),
    onSuccess: () => irParaLogin('senha-alterada'),
  })
}
