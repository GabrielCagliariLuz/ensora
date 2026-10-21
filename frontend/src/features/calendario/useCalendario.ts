import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { carregarCalendario } from '../../api/calendario'
import { chavesCalendario } from './chaves'

export function useCalendario(de: string, ate: string) {
  return useQuery({
    queryKey: chavesCalendario.intervalo(de, ate),
    queryFn: ({ signal }) => carregarCalendario(de, ate, signal),
    // Ao trocar de mês, os dados do anterior ficam na tela até os novos chegarem,
    // então a grade não pisca.
    placeholderData: keepPreviousData,
  })
}
