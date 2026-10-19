import { useQuery } from '@tanstack/react-query'
import { listarHabitos } from '../../api/habitos'
import { chavesHabitos } from './chaves'

// `ativo` permite um componente sempre montado (como o sheet de Gerenciar)
// só buscar a lista quando for exibida.
export function useHabitos(ativo = true) {
  return useQuery({
    queryKey: chavesHabitos.todos,
    queryFn: ({ signal }) => listarHabitos(signal),
    enabled: ativo,
  })
}
