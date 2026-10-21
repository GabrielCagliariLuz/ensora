import type { Registro } from './agenda'
import { corDoRegistro } from './cores'

export const PONTOS_MAXIMOS = 3

// Cores dos pontos sob o número do dia: uma por registro, na ordem do dia, até
// PONTOS_MAXIMOS. Sem número de registros: o ponto só diz que há algo no dia.
export function pontosDoDia(registros: Registro[], agora: Date): string[] {
  return registros
    .slice(0, PONTOS_MAXIMOS)
    .map((registro) => corDoRegistro(registro, agora))
}
