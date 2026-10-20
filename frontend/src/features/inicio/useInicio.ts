import { useQuery } from '@tanstack/react-query'
import { carregarInicio } from '../../api/inicio'
import { chavesInicio } from './chaves'

export function useInicio(data: string) {
  return useQuery({
    queryKey: chavesInicio.dia(data),
    queryFn: ({ signal }) => carregarInicio(data, signal),
  })
}
