import type { DiaDoRitmo, Ritmo } from '../../api/tipos'
import { diaDaSemana, diaDoMes, somarDias } from '../../lib/datas'
import { mesDoDia, somarMeses } from '../calendario/grade'

export interface IntervaloDoRitmo {
  de: string
  ate: string
}

export function ultimoDiaDoMes(mes: string): string {
  return somarDias(`${somarMeses(mes, 1)}-01`, -1)
}

// O intervalo que vai em GET /insights/ritmo: o mês todo, ou, no mês em andamento,
// só até hoje (o servidor conta todo dia do intervalo como já decorrido). Mês
// que ainda não começou não tem ritmo: devolve null e a tela não consulta.
export function intervaloDoRitmo(
  mes: string,
  hoje: string,
): IntervaloDoRitmo | null {
  const mesAtual = mesDoDia(hoje)
  if (mes > mesAtual) return null // 'AAAA-MM' compara como texto

  return {
    de: `${mes}-01`,
    ate: mes === mesAtual ? hoje : ultimoDiaDoMes(mes),
  }
}

export interface BarraSemanal {
  rotulo: string
  de: string
  ate: string
  // Sem valor: a semana ainda não começou, ou nenhum hábito existia nela.
  ritmo?: number
  // Hoje cai nesta semana.
  atual: boolean
}

// As quatro barras S1 a S4 sempre existem, mesmo que o servidor não traga a
// semana (mês em andamento): a barra sem valor aparece vazia. Os blocos são os
// do ritmo (decisão 57): dias 1-7, 8-14, 15-21 e 22 até o fim do mês.
export function barrasSemanais(
  mes: string,
  ritmo: Ritmo | undefined,
  hoje: string,
): BarraSemanal[] {
  const dia = (numero: number) => `${mes}-${String(numero).padStart(2, '0')}`
  const blocos: [string, string][] = [
    [dia(1), dia(7)],
    [dia(8), dia(14)],
    [dia(15), dia(21)],
    [dia(22), ultimoDiaDoMes(mes)],
  ]

  return blocos.map(([de, ate], indice) => {
    const rotulo = `S${indice + 1}`
    return {
      rotulo,
      de,
      ate,
      ritmo: ritmo?.semanas.find((semana) => semana.rotulo === rotulo)?.ritmo,
      atual: hoje >= de && hoje <= ate,
    }
  })
}

// '01–07'.
export function rotuloDoIntervalo(de: string, ate: string): string {
  return `${diaDoMes(de)}–${diaDoMes(ate)}`
}

const DIAS = ['SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB', 'DOM']
const NOMES_DOS_DIAS = [
  'Segunda',
  'Terça',
  'Quarta',
  'Quinta',
  'Sexta',
  'Sábado',
  'Domingo',
]

export interface MediaDoDia {
  chave: string
  // 'S', 'T', 'Q', 'Q', 'S', 'S', 'D'.
  inicial: string
  nome: string
  // Média do ritmo nesse dia da semana; sem valor se nenhum dia teve ritmo.
  media?: number
}

// A média do ritmo de cada dia da semana (segunda a domingo) no período, só com
// os dias que têm ritmo (dia sem hábito existente fica fora).
export function ritmoPorDiaDaSemana(dias: DiaDoRitmo[]): MediaDoDia[] {
  const acumulado = DIAS.map(() => ({ soma: 0, quantidade: 0 }))

  for (const dia of dias) {
    if (dia.ritmo === undefined) continue
    const indice = DIAS.indexOf(diaDaSemana(dia.data))
    if (indice < 0) continue
    acumulado[indice].soma += dia.ritmo
    acumulado[indice].quantidade += 1
  }

  return DIAS.map((chave, indice) => ({
    chave,
    inicial: chave[0],
    nome: NOMES_DOS_DIAS[indice],
    media:
      acumulado[indice].quantidade > 0
        ? acumulado[indice].soma / acumulado[indice].quantidade
        : undefined,
  }))
}

export interface MelhorEPior {
  melhor: MediaDoDia
  pior: MediaDoDia
}

// O dia da semana mais forte e o mais fraco. Sem pelo menos dois dias com valor,
// ou com todos iguais, não há o que comparar (null). No empate vale o primeiro,
// na ordem segunda a domingo.
export function melhorEPiorDia(medias: MediaDoDia[]): MelhorEPior | null {
  const comValor = medias.filter((dia) => dia.media !== undefined)
  if (comValor.length < 2) return null

  let melhor = comValor[0]
  let pior = comValor[0]
  for (const dia of comValor) {
    if ((dia.media ?? 0) > (melhor.media ?? 0)) melhor = dia
    if ((dia.media ?? 0) < (pior.media ?? 0)) pior = dia
  }
  return melhor.media === pior.media ? null : { melhor, pior }
}

// '82%'. O servidor manda uma casa decimal; a tela mostra o inteiro.
export function percentual(valor: number | undefined): string {
  return valor === undefined ? '–' : `${Math.round(valor)}%`
}
