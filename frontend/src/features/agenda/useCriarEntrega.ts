import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarEntrega } from '../../api/entregas'
import { invalidarDadosDeAgenda } from './invalidarAgenda'

export function useCriarEntrega() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarEntrega,
    // Devolver a promessa mantém a mutação pendente até o refetch terminar,
    // então o Upcoming já está atualizado quando o formulário fecha.
    onSuccess: () => invalidarDadosDeAgenda(queryClient),
  })
}
