const MESES_ABREVIADOS = [
  'JAN',
  'FEV',
  'MAR',
  'ABR',
  'MAI',
  'JUN',
  'JUL',
  'AGO',
  'SET',
  'OUT',
  'NOV',
  'DEZ',
]

// '2026-09-26' vira '26 SET'. As abreviações são próprias porque o Intl varia
// entre ambientes (por exemplo, "set." com ponto). Uma data fora do formato
// volta como veio.
export function rotuloDia(iso: string): string {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (partes === null) return iso

  const mes = MESES_ABREVIADOS[Number(partes[2]) - 1]
  if (mes === undefined) return iso
  return `${partes[3]} ${mes}`
}

// Único lugar que decide "hoje" no front: a data local do aparelho, no
// formato AAAA-MM-DD que a API espera. Não usa toISOString(), que é UTC e
// devolveria o dia seguinte à noite no Brasil.
export function hojeISO(agora: Date = new Date()): string {
  const ano = agora.getFullYear()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

function dataEmUTC(iso: string): number {
  const [ano, mes, dia] = iso.split('-').map(Number)
  return Date.UTC(ano, mes - 1, dia)
}

// Quantos dias de calendário há de `de` até `ate` (AAAA-MM-DD): positivo se
// `ate` é depois. Conta em UTC para o horário de verão não gerar 23 ou 25
// horas no meio.
export function diasEntre(de: string, ate: string): number {
  return Math.round((dataEmUTC(ate) - dataEmUTC(de)) / 86_400_000)
}

// '2026-10-04' vira '04 out', o formato dos cartões da Home.
export function dataCurta(iso: string): string {
  const rotulo = rotuloDia(iso)
  return rotulo === iso ? iso : rotulo.toLowerCase()
}

// '2026-10-04' vira '04'.
export function diaDoMes(iso: string): string {
  return /^\d{4}-\d{2}-(\d{2})$/.exec(iso)?.[1] ?? ''
}

const DIAS_DA_SEMANA = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB']

// '2026-10-23' vira 'SEX'. Data fora do formato devolve texto vazio.
export function diaDaSemana(iso: string): string {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (partes === null) return ''

  const data = new Date(
    Number(partes[1]),
    Number(partes[2]) - 1,
    Number(partes[3]),
  )
  return DIAS_DA_SEMANA[data.getDay()]
}
