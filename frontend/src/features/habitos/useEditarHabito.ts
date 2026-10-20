import { useMutation, useQueryClient } from '@tanstack/react-query'
import { editarHabito } from '../../api/habitos'
import type { HabitoRequest } from '../../api/tipos'
import { invalidarDadosDeHabitos } from './invalidarHabitos'

interface EdicaoHabito {
  id: number
  corpo: HabitoRequest
}

export function useEditarHabito() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, corpo }: EdicaoHabito) => editarHabito(id, corpo),
    onSuccess: () => invalidarDadosDeHabitos(queryClient),
  })
}
