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
