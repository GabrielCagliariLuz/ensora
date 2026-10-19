// Único lugar que decide "hoje" no front: a data local do aparelho, no
// formato AAAA-MM-DD que a API espera. Não usa toISOString(), que é UTC e
// devolveria o dia seguinte à noite no Brasil.
export function hojeISO(agora: Date = new Date()): string {
  const ano = agora.getFullYear()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}
