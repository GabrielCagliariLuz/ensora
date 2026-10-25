import { useMutation, useQueryClient } from '@tanstack/react-query'
import { concluirIntroducao } from '../../api/conta'
import { chavesConta } from '../ajustes/chaves'

// Grava no cache a conta devolvida (já com introducaoConcluida), para o tutorial
// não voltar enquanto a sessão durar.
export function useConcluirIntroducao() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: concluirIntroducao,
    onSuccess: (conta) => {
      queryClient.setQueryData(chavesConta.atual, conta)
    },
  })
}
