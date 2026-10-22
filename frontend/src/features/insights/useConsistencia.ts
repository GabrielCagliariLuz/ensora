import { useEffect } from 'react'
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { carregarConsistencia } from '../../api/insights'
import { somarMeses } from '../calendario/grade'
import { chavesInsights } from './chaves'

export function useConsistencia(mes: string) {
  return useQuery({
    queryKey: chavesInsights.consistencia(mes),
    queryFn: ({ signal }) => carregarConsistencia(mes, signal),
    // Ao trocar de mês, a grade anterior fica na tela até a nova chegar.
    placeholderData: keepPreviousData,
  })
}

// Com o mês na tela, busca os dois vizinhos em segundo plano: as setas já
// encontram os dados e a troca não espera a rede.
export function usePrefetchMesesVizinhos(mes: string) {
  const queryClient = useQueryClient()

  useEffect(() => {
    for (const vizinho of [somarMeses(mes, -1), somarMeses(mes, 1)]) {
      void queryClient.prefetchQuery({
        queryKey: chavesInsights.consistencia(vizinho),
        queryFn: ({ signal }) => carregarConsistencia(vizinho, signal),
        // Dentro deste prazo não busca de novo ao andar de mês para trás e para
        // frente.
        staleTime: 60_000,
      })
    }
  }, [mes, queryClient])
}
