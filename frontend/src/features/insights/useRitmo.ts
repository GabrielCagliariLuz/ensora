import { useEffect } from 'react'
import {
  keepPreviousData,
  skipToken,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { carregarRitmo } from '../../api/insights'
import { somarMeses } from '../calendario/grade'
import { chavesInsights } from './chaves'
import { intervaloDoRitmo } from './ritmo'

// O ritmo do mês. Num mês que ainda não começou não há consulta: o intervalo
// vem null, e quem usa mostra "ainda não começou" em vez dos dados.
export function useRitmo(mes: string, hoje: string) {
  const intervalo = intervaloDoRitmo(mes, hoje)

  const consulta = useQuery({
    queryKey: intervalo
      ? chavesInsights.ritmo(intervalo.de, intervalo.ate)
      : chavesInsights.ritmoFuturo(mes),
    queryFn: intervalo
      ? ({ signal }) => carregarRitmo(intervalo.de, intervalo.ate, signal)
      : skipToken,
    // Ao trocar de mês, o ritmo anterior fica na tela até o novo chegar.
    placeholderData: keepPreviousData,
  })

  return { intervalo, consulta }
}

// Busca em segundo plano o ritmo dos dois meses vizinhos (o futuro não tem).
export function usePrefetchRitmoVizinho(mes: string, hoje: string) {
  const queryClient = useQueryClient()

  useEffect(() => {
    for (const vizinho of [somarMeses(mes, -1), somarMeses(mes, 1)]) {
      const intervalo = intervaloDoRitmo(vizinho, hoje)
      if (intervalo === null) continue
      void queryClient.prefetchQuery({
        queryKey: chavesInsights.ritmo(intervalo.de, intervalo.ate),
        queryFn: ({ signal }) => carregarRitmo(intervalo.de, intervalo.ate, signal),
        staleTime: 60_000,
      })
    }
  }, [mes, hoje, queryClient])
}
