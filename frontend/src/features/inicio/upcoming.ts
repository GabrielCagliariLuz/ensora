import type { Entrega, Evento } from '../../api/tipos'
import { dataCurta, diaDaSemana, diasEntre, hojeISO } from '../../lib/datas'

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

// 'HH:mm:ss' do relógio local, no mesmo formato dos horários da API: dá para
// compará-los como texto.
function horaDe(agora: Date): string {
  return [
    doisDigitos(agora.getHours()),
    doisDigitos(agora.getMinutes()),
    doisDigitos(agora.getSeconds()),
  ].join(':')
}

// '14:30:00' vira '14h30'; '09:00:00' vira '9h'.
export function formatarHora(hora: string): string {
  const [horas, minutos] = hora.split(':')
  return minutos === '00' ? `${Number(horas)}h` : `${Number(horas)}h${minutos}`
}

export type EstadoEvento = 'normal' | 'agora' | 'encerrado'

// Onde o evento está no dia, pelo relógio do aparelho: ainda vai acontecer,
// está acontecendo (entre o início e o término) ou já passou. Sem horário de
// término não há como saber quando acabou, então ele encerra ao começar. Dias
// à frente são sempre normais.
export function estadoEvento(evento: Evento, agora: Date): EstadoEvento {
  const hoje = hojeISO(agora)
  if (evento.data > hoje) return 'normal'
  if (evento.data < hoje) return 'encerrado'

  const hora = horaDe(agora)
  if (hora < evento.inicio) return 'normal'
  if (evento.termino === undefined) return 'encerrado'
  return hora < evento.termino ? 'agora' : 'encerrado'
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

// 'qui, 04 out · até 14h30', ou só 'qui, 04 out' quando não há horário.
export function legendaPrazo(entrega: Entrega): string {
  const data = `${diaDaSemana(entrega.prazoData).toLowerCase()}, ${dataCurta(entrega.prazoData)}`
  return entrega.prazoHorario
    ? `${data} · até ${formatarHora(entrega.prazoHorario)}`
    : data
}

// Dias que faltam (negativo se já passou) e se o prazo já venceu: a regra de
// atraso, usada pelas duas leituras abaixo. Calculada com o relógio do
// aparelho, pois o servidor não conhece a hora local.
function prazoDe(entrega: Entrega, agora: Date) {
  const dias = diasEntre(hojeISO(agora), entrega.prazoData)
  const horaAgora = horaDe(agora)
  const limite = entrega.prazoHorario ?? PRAZO_SEM_HORARIO
  // Horas até o fim do prazo (negativo se já passou), para as faixas de alerta.
  const horas =
    (new Date(`${entrega.prazoData}T${limite}`).getTime() - agora.getTime()) /
    3_600_000
  return {
    dias,
    horas,
    venceu: dias < 0 || (dias === 0 && horaAgora > limite),
  }
}

// "Atrasada" e "vence em N dias", por extenso (detalhes da entrega).
export function situacaoPrazo(entrega: Entrega, agora: Date): SituacaoPrazo {
  if (entrega.concluida) return { texto: 'concluída', urgente: false }

  const { dias, venceu } = prazoDe(entrega, agora)
  if (venceu) {
    if (dias === 0) return { texto: 'venceu hoje', urgente: true }
    if (dias === -1) return { texto: 'venceu ontem', urgente: true }
    return { texto: `venceu há ${-dias} dias`, urgente: true }
  }
  if (dias === 0) return { texto: 'vence hoje', urgente: true }
  if (dias === 1) return { texto: 'vence amanhã', urgente: true }
  return { texto: `vence em ${dias} dias`, urgente: false }
}

export type EstadoPrazo =
  | 'normal'
  | 'atencao'
  | 'urgente'
  | 'atrasada'
  | 'concluida'

export interface ContagemPrazo {
  texto: string
  // Escolhe o ícone e a cor do cartão: atenção é entre 48h e 24h para o prazo
  // (amarelo), urgente é menos de 24h (ember) e atrasada já passou.
  estado: EstadoPrazo
  // Segunda linha, mais discreta (só nas atrasadas: desde quando).
  apoio?: string
  urgente: boolean
}

// A versão curta da situação, para a contagem à direita do cartão da Home:
// '3 dias', 'amanhã', 'hoje' ou 'atrasada'. A cor segue as horas que faltam, não
// o dia do calendário: menos de 24h é urgente, de 24h a 48h é atenção.
export function contagemPrazo(entrega: Entrega, agora: Date): ContagemPrazo {
  if (entrega.concluida) {
    return { texto: 'concluída', estado: 'concluida', urgente: false }
  }

  const { dias, horas, venceu } = prazoDe(entrega, agora)
  if (venceu) {
    const apoio =
      dias === 0 ? 'desde hoje' : dias === -1 ? 'desde ontem' : `há ${-dias} dias`
    return { texto: 'atrasada', apoio, estado: 'atrasada', urgente: true }
  }
  const texto = dias === 0 ? 'hoje' : dias === 1 ? 'amanhã' : `${dias} dias`
  if (horas < 24) return { texto, estado: 'urgente', urgente: true }
  if (horas < 48) return { texto, estado: 'atencao', urgente: false }
  return { texto, estado: 'normal', urgente: false }
}

// Pendentes primeiro, concluídas depois, cada grupo na ordem do servidor
// (data, horário). Concluir faz a entrega descer; desfazer, subir.
export function ordenarEntregas(entregas: readonly Entrega[]): Entrega[] {
  return [
    ...entregas.filter((entrega) => !entrega.concluida),
    ...entregas.filter((entrega) => entrega.concluida),
  ]
}
