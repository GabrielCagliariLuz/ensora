import { useQuery } from '@tanstack/react-query'
import { carregarConfiguracaoCadastro } from '../../api/auth'

// O cadastro está aberto? Pede convite? Serve ao link "Criar conta" do login e à
// própria tela de cadastro. Muda raramente, então vale por um minuto.
export function useConfiguracaoCadastro() {
  return useQuery({
    queryKey: ['auth', 'registro'],
    queryFn: ({ signal }) => carregarConfiguracaoCadastro(signal),
    staleTime: 60_000,
  })
}
