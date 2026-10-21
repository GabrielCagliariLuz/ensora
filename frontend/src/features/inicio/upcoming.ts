import type { Entrega } from '../../api/tipos'
import { dataCurta, diasEntre, hojeISO } from '../../lib/datas'

// Sem horário cadastrado, a entrega vale até o fim do dia.
const PRAZO_SEM_HORARIO = '23:59:00'

export interface SituacaoPrazo {
  texto: string
  // Cor de urgência: só entregas, e só atrasada, hoje ou amanhã.
  urgente: boolean
}

function doisDigitos(numero: number): string {
  return String(numero).padStart(2, '0')
}

// '14:30:00' vira '14h30'; '09:00:00' vira '9h'.
export function formatarHora(hora: string): string {
  const [horas, minutos] = hora.split(':')
  return minutos === '00' ? `${Number(horas)}h` : `${Number(horas)}h${minutos}`
}

// '20h30' ou, com término, '19h a 22h'.
export function formatarHorarioEvento(inicio: string, termino?: string): string {
  return termino
    ? `${formatarHora(inicio)} a ${formatarHora(termino)}`
    : formatarHora(inicio)
}

// Evento passado não chega à Home, então só hoje, amanhã e N dias importam.
export function textoDiasEvento(data: string, hoje: string): string {
  const dias = diasEntre(hoje, data)
  if (dias <= 0) return 'hoje'
  if (dias === 1) return 'amanhã'
  return `${dias} dias`
}

// '04 out' ou '04 out, 14h30' quando a entrega tem horário.
export function rotuloPrazo(entrega: Entrega): string {
  const data = dataCurta(entrega.prazoData)
  return entrega.prazoHorario
    ? `${data}, ${formatarHora(entrega.prazoHorario)}`
    : data
}

// "Atrasada" e "vence em N dias" são calculados aqui, com o relógio do
// aparelho: o servidor não conhece a hora local.
export function situacaoPrazo(entrega: Entrega, agora: Date): SituacaoPrazo {
  if (entrega.concluida) return { texto: 'concluída', urgente: false }

  const dias = diasEntre(hojeISO(agora), entrega.prazoData)
  const horaAgora = [
    doisDigitos(agora.getHours()),
    doisDigitos(agora.getMinutes()),
    doisDigitos(agora.getSeconds()),
  ].join(':')
  const limite = entrega.prazoHorario ?? PRAZO_SEM_HORARIO
  const venceu = dias < 0 || (dias === 0 && horaAgora > limite)

  if (venceu) {
    if (dias === 0) return { texto: 'venceu hoje', urgente: true }
    if (dias === -1) return { texto: 'venceu ontem', urgente: true }
    return { texto: `venceu há ${-dias} dias`, urgente: true }
  }
  if (dias === 0) return { texto: 'vence hoje', urgente: true }
  if (dias === 1) return { texto: 'vence amanhã', urgente: true }
  return { texto: `vence em ${dias} dias`, urgente: false }
}

// Pendentes primeiro, concluídas depois, cada grupo na ordem do servidor
// (data, horário). Concluir faz a entrega descer; desfazer, subir.
export function ordenarEntregas(entregas: readonly Entrega[]): Entrega[] {
  return [
    ...entregas.filter((entrega) => !entrega.concluida),
    ...entregas.filter((entrega) => entrega.concluida),
  ]
}
