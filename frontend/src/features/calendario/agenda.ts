import type { Calendario, Entrega, Evento } from '../../api/tipos'

export interface RegistrosDoDia {
  entregas: Entrega[]
  eventos: Evento[]
}

// Um item da lista do dia: evento ou entrega.
export type Registro =
  | { tipo: 'EVENTO'; evento: Evento }
  | { tipo: 'ENTREGA'; entrega: Entrega }

// '09:00:00' vira '09:00'. Os horários saem do servidor com segundos.
export function horaCurta(hora: string): string {
  return hora.slice(0, 5)
}

// As listas planas do servidor viram um mapa por dia (AAAA-MM-DD): as entregas
// pelo prazo e os eventos pela data. Dias sem registro não entram.
export function agruparPorDia(
  calendario: Pick<Calendario, 'entregas' | 'eventos'>,
): Map<string, RegistrosDoDia> {
  const dias = new Map<string, RegistrosDoDia>()

  function doDia(dia: string): RegistrosDoDia {
    const existente = dias.get(dia)
    if (existente) return existente
    const novo: RegistrosDoDia = { entregas: [], eventos: [] }
    dias.set(dia, novo)
    return novo
  }

  for (const entrega of calendario.entregas) {
    doDia(entrega.prazoData).entregas.push(entrega)
  }
  for (const evento of calendario.eventos) {
    doDia(evento.data).eventos.push(evento)
  }
  return dias
}

// Sem horário, a entrega fica no fim do dia.
const SEM_HORARIO = '99:99:99'

// Eventos e entregas do dia na mesma lista, por horário. Num empate o evento
// vem antes, e a ordem do servidor vale dentro de cada tipo.
export function registrosDoDia(grupo: RegistrosDoDia | undefined): Registro[] {
  if (grupo === undefined) return []

  const registros: { chave: string; registro: Registro }[] = [
    ...grupo.eventos.map((evento) => ({
      chave: evento.inicio,
      registro: { tipo: 'EVENTO', evento } as const,
    })),
    ...grupo.entregas.map((entrega) => ({
      chave: entrega.prazoHorario ?? SEM_HORARIO,
      registro: { tipo: 'ENTREGA', entrega } as const,
    })),
  ]

  // O sort é estável: no empate, fica a ordem acima (eventos antes).
  return registros
    .sort((a, b) => a.chave.localeCompare(b.chave))
    .map(({ registro }) => registro)
}
