import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarHabito } from '../../api/habitos'
import { invalidarDadosDeHabitos } from './invalidarHabitos'

export function useCriarHabito() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarHabito,
    // Devolver a promessa mantém a mutação pendente até o refetch terminar,
    // então a lista e a Home já estão atualizadas quando o formulário fecha.
    onSuccess: () => invalidarDadosDeHabitos(queryClient),
  })
}
