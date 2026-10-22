import { useQuery } from '@tanstack/react-query'
import { carregarConta } from '../../api/conta'
import { chavesConta } from './chaves'

export function useConta() {
  return useQuery({
    queryKey: chavesConta.atual,
    queryFn: ({ signal }) => carregarConta(signal),
  })
}
