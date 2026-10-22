import { useMutation, useQueryClient } from '@tanstack/react-query'
import { alterarFuso } from '../../api/conta'
import { chavesInsights } from '../insights/chaves'
import { chavesConta } from './chaves'

export function useAlterarFuso() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: alterarFuso,
    onSuccess: (conta) => {
      queryClient.setQueryData(chavesConta.atual, conta)
      // O "hoje" e os dias de criação dos Insights dependem do fuso.
      return queryClient.invalidateQueries({ queryKey: chavesInsights.todos })
    },
  })
}
