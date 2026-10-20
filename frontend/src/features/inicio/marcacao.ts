import type { QueryClient } from '@tanstack/react-query'
import type { HabitoNaHome, Inicio } from '../../api/tipos'
import { chavesInicio } from './chaves'

function marcar(habito: HabitoNaHome, feito: boolean): HabitoNaHome {
  // Diário não tem contador.
  if (habito.feitos === undefined) return { ...habito, feito }
  return {
    ...habito,
    feito,
    feitos: Math.max(0, habito.feitos + (feito ? 1 : -1)),
  }
}

// Resultado da marcação no `Inicio`, sem alterar o original. Só muda se o
// estado do hábito de fato muda: marcar o que já está marcado devolve o mesmo
// objeto, e o contador semanal não é distorcido por um clique redundante.
export function aplicarMarcacao(
  inicio: Inicio,
  id: number,
  feito: boolean,
): Inicio {
  const alvo = inicio.habitos.find((habito) => habito.id === id)
  if (alvo === undefined || alvo.feito === feito) return inicio

  return {
    ...inicio,
    habitos: inicio.habitos.map((habito) =>
      habito.id === id ? marcar(habito, feito) : habito,
    ),
  }
}

// Aplica a marcação no cache do dia. Sem dados em cache (ainda carregando),
// não faz nada. Serve para marcar e, com o estado oposto, para reverter só
// aquele hábito, sem desfazer marcações de outros que ainda estão em voo.
export function aplicarMarcacaoNoCache(
  queryClient: QueryClient,
  data: string,
  id: number,
  feito: boolean,
): void {
  queryClient.setQueryData<Inicio>(chavesInicio.dia(data), (atual) =>
    atual === undefined ? undefined : aplicarMarcacao(atual, id, feito),
  )
}
