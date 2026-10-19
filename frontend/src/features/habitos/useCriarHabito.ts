import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarHabito } from '../../api/habitos'
import { chavesHabitos } from './chaves'

export function useCriarHabito() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarHabito,
    // Devolver a promessa mantém a mutação pendente até o refetch terminar,
    // então a lista já está atualizada quando o formulário fecha.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: chavesHabitos.todos }),
  })
}
