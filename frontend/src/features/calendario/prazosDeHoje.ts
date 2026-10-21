import type { Entrega } from '../../api/tipos'
import { ordenarEntregas } from '../inicio/upcoming'
import type { RegistrosDoDia } from './agenda'
import { gradeDoMes } from './grade'

// Sem horário, a entrega fica depois das que têm.
const SEM_HORARIO = '99:99:99'

function chave(entrega: Entrega): string {
  return `${entrega.prazoData} ${entrega.prazoHorario ?? SEM_HORARIO}`
}

// Hoje está na grade do mês visível? Fora dela não há dados de hoje.
export function hojeNaGrade(mes: string, hoje: string): boolean {
  return gradeDoMes(mes).includes(hoje)
}

// Entregas com prazo hoje (as concluídas ficam por último) e as atrasadas ainda
// pendentes, em ordem de prazo. Os dados são os da grade visível: se hoje não
// está nela (outro mês), devolve vazio, e atrasadas de antes da grade não entram.
export function prazosDeHoje(
  dias: Map<string, RegistrosDoDia>,
  mes: string,
  hoje: string,
): Entrega[] {
  if (!hojeNaGrade(mes, hoje)) return []

  const entregas: Entrega[] = []
  for (const [dia, grupo] of dias) {
    if (dia > hoje) continue
    for (const entrega of grupo.entregas) {
      if (dia === hoje || !entrega.concluida) entregas.push(entrega)
    }
  }

  entregas.sort((a, b) => chave(a).localeCompare(chave(b)))
  return ordenarEntregas(entregas)
}
