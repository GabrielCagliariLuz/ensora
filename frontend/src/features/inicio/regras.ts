export interface ProgressoSemanal {
  feitos: number
  meta: number
}

// Contador do hábito semanal no cartão. A meta pode ser ultrapassada, então
// 4/3 é um valor válido e não é limitado.
export function textoProgresso({ feitos, meta }: ProgressoSemanal): string {
  return `${feitos}/${meta}`
}
