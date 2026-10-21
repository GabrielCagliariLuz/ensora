import { useEffect } from 'react'
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { carregarCalendario } from '../../api/calendario'
import { chavesCalendario } from './chaves'
import { intervaloDaGrade, somarMeses } from './grade'

export function useCalendario(de: string, ate: string) {
  return useQuery({
    queryKey: chavesCalendario.intervalo(de, ate),
    queryFn: ({ signal }) => carregarCalendario(de, ate, signal),
    // Ao trocar de mês, os dados do anterior ficam na tela até os novos chegarem,
    // então a grade não pisca.
    placeholderData: keepPreviousData,
  })
}

// Com o mês na tela, busca os dois vizinhos em segundo plano: as setas já
// encontram os dados e a troca de mês não espera a rede.
export function usePrefetchMesesVizinhos(mes: string) {
  const queryClient = useQueryClient()

  useEffect(() => {
    for (const vizinho of [somarMeses(mes, -1), somarMeses(mes, 1)]) {
      const { de, ate } = intervaloDaGrade(vizinho)
      void queryClient.prefetchQuery({
        queryKey: chavesCalendario.intervalo(de, ate),
        queryFn: ({ signal }) => carregarCalendario(de, ate, signal),
        // Dentro deste prazo não busca de novo ao andar de mês para trás e para
        // frente.
        staleTime: 60_000,
      })
    }
  }, [mes, queryClient])
}
