import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { entrar, registrar } from '../../api/auth'
import type { CadastroRequest } from '../../api/tipos'
import { CAMINHOS } from '../../lib/caminhos'

// Cria a conta e já entra com as mesmas credenciais: a pessoa cai na Home.
export function useCadastrar() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async (dados: CadastroRequest) => {
      await registrar(dados)
      await entrar({ email: dados.email, senha: dados.senha })
    },
    onSuccess: () => {
      void navigate(CAMINHOS.inicio, { replace: true })
    },
  })
}
