import type { HabitoInsights } from '../../api/tipos'

// A partir deste percentual o anel fica verde (como no Figma, onde só os
// primeiros colocados têm o anel verde).
export const LIMITE_DESTAQUE = 90

// Dias feitos e dias esperados: a conta que gera o percentual do anel.
export interface Contagem {
  feitos: number
  total: number
}

// De onde vem a conta do percentual, sempre em dias. O diário conta os dias
// concluídos sobre os dias considerados (com o de hoje em aberto entrando na
// base). O semanal soma, nas semanas que entram na conta, os dias feitos de
// cada uma, limitados à meta, sobre a soma das metas (seção 7.2, decisão 59).
// Sem nada a considerar (hábito de hoje, mês futuro), devolve null.
export function contagemDoHabito(habito: HabitoInsights): Contagem | null {
  if (habito.tipo === 'SEMANAL') {
    let feitos = 0
    let total = 0
    for (const semana of habito.semanas ?? []) {
      if (!semana.contaNaConsistencia) continue
      total += semana.meta
      feitos += Math.min(semana.feitos, semana.meta)
    }
    return total === 0 ? null : { feitos, total }
  }

  let feitos = 0
  let total = 0
  for (const celula of habito.dias) {
    if (celula.estado === 'DESCANSO_FUTURO') continue
    total += 1
    if (celula.estado === 'CONCLUIDO') feitos += 1
  }
  return total === 0 ? null : { feitos, total }
}

// '20 / 20 dias', '7 / 9 dias'.
export function textoDaContagem(contagem: Contagem | null): string {
  if (contagem === null) return 'Sem dados ainda'
  return `${contagem.feitos} / ${contagem.total} dias`
}

// A diferença, em pontos percentuais, entre a consistência deste mês e a do mês
// anterior, nos números inteiros que a tela mostra (assim "87%" contra "79%" é
// sempre +8). Sem um dos dois lados, não há comparação (null).
export function deltaEmPontos(
  atual: number | undefined,
  anterior: number | undefined,
): number | null {
  if (atual === undefined || anterior === undefined) return null
  return Math.round(atual) - Math.round(anterior)
}

// '+7 pts', '−3 pts' (com o sinal de menos de verdade) e '0 pts'.
export function textoDoDelta(delta: number): string {
  if (delta > 0) return `+${delta} pts`
  if (delta < 0) return `−${Math.abs(delta)} pts`
  return '0 pts'
}

export interface CartaoDoHabito {
  id: number
  nome: string
  excluido: boolean
  consistencia?: number
  contagem: Contagem | null
  // Pontos a mais ou a menos que no mês anterior; null sem comparação.
  delta: number | null
}

// Um cartão por hábito do mês, do mais para o menos consistente (sem valor por
// último; no empate, o nome, sem diferenciar acentos). O delta usa o mesmo
// hábito (mesmo id) no mês anterior.
export function montarCartoes(
  atual: HabitoInsights[],
  anterior: HabitoInsights[] | undefined,
): CartaoDoHabito[] {
  const cartoes = atual.map((habito) => ({
    id: habito.id,
    nome: habito.nome,
    excluido: habito.excluido,
    consistencia: habito.consistencia,
    contagem: contagemDoHabito(habito),
    delta: deltaEmPontos(
      habito.consistencia,
      anterior?.find((antigo) => antigo.id === habito.id)?.consistencia,
    ),
  }))

  return cartoes.sort((a, b) => {
    if (a.consistencia === undefined && b.consistencia !== undefined) return 1
    if (b.consistencia === undefined && a.consistencia !== undefined) return -1
    if (a.consistencia !== undefined && b.consistencia !== undefined) {
      const diferenca = b.consistencia - a.consistencia
      if (diferenca !== 0) return diferenca
    }
    return (
      a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' }) ||
      a.id - b.id
    )
  })
}
