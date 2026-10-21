import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarEvento } from '../../api/eventos'
import { invalidarDadosDeAgenda } from './invalidarAgenda'

export function useCriarEvento() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: criarEvento,
    onSuccess: () => invalidarDadosDeAgenda(queryClient),
  })
}
