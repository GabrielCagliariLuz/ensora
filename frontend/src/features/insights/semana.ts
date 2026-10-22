import { diaDaSemana, diaDoMes, rotuloDoMes, somarDias } from '../../lib/datas'
import { mesDoDia, somarMeses } from '../calendario/grade'

// A semana dos Insights vai de segunda a domingo, como no backend (seção 7.2).

// Todos os dias do mês ('AAAA-MM'), do dia 1 ao último.
export function diasDoMes(mes: string): string[] {
  const ultimo = somarDias(`${somarMeses(mes, 1)}-01`, -1)
  return Array.from({ length: Number(diaDoMes(ultimo)) }, (_, indice) =>
    `${mes}-${String(indice + 1).padStart(2, '0')}`,
  )
}

// 'D', 'S', 'T', 'Q', 'Q', 'S', 'S': a inicial do dia da semana, como nos mocks.
export function inicialDoDia(iso: string): string {
  return diaDaSemana(iso).charAt(0)
}

// Segunda-feira abre a semana (a coluna ganha uma linha mais forte).
export function ehSegunda(iso: string): boolean {
  return diaDaSemana(iso) === 'SEG'
}

// Sábado e domingo (a coluna ganha um fundo levemente tingido).
export function ehFimDeSemana(iso: string): boolean {
  const dia = diaDaSemana(iso)
  return dia === 'SAB' || dia === 'DOM'
}

// A segunda-feira da semana do dia (AAAA-MM-DD).
export function segundaDaSemana(iso: string): string {
  const [ano, mes, dia] = iso.split('-').map(Number)
  const indiceDoDia = new Date(ano, mes - 1, dia).getDay() // 0 = domingo
  return somarDias(iso, -((indiceDoDia + 6) % 7))
}

// Os 7 dias da semana, de segunda a domingo.
export function diasDaSemana(segunda: string): string[] {
  return Array.from({ length: 7 }, (_, indice) => somarDias(segunda, indice))
}

// Os meses ('AAAA-MM') que a semana toca: um, ou dois quando ela cruza a virada
// do mês. Cada um vira uma consulta à consistência.
export function mesesDaSemana(dias: string[]): string[] {
  const primeiro = mesDoDia(dias[0])
  const ultimo = mesDoDia(dias[dias.length - 1])
  return primeiro === ultimo ? [primeiro] : [primeiro, ultimo]
}

function nomeDoMes(mes: string): string {
  return rotuloDoMes(mes).split(' ')[0]
}

// '21 – 27 Setembro'; numa semana que cruza o mês, '28 Set – 4 Out'.
export function rotuloDaSemana(dias: string[]): string {
  const primeiro = dias[0]
  const ultimo = dias[dias.length - 1]
  const diaInicial = Number(diaDoMes(primeiro))
  const diaFinal = Number(diaDoMes(ultimo))

  if (mesDoDia(primeiro) === mesDoDia(ultimo)) {
    return `${diaInicial} – ${diaFinal} ${nomeDoMes(mesDoDia(primeiro))}`
  }
  const abreviado = (mes: string) => nomeDoMes(mes).slice(0, 3)
  return `${diaInicial} ${abreviado(mesDoDia(primeiro))} – ${diaFinal} ${abreviado(mesDoDia(ultimo))}`
}

// 'Semana 3' para a semana que começa no dia 21. É a mesma divisão do ritmo
// (S1 a S4, dias 1-7, 8-14, 15-21 e 22 em diante): a segunda-feira decide, e a
// que cai nos dias 29 a 31 ainda é a quarta.
export function numeroDaSemana(segunda: string): number {
  return Math.min(4, Math.floor((Number(diaDoMes(segunda)) - 1) / 7) + 1)
}
