import { requisitar } from './cliente'
import type { Consistencia } from './tipos'

// A grade hábito × dia e a consistência de um mês ('AAAA-MM'). Qualquer mês é
// aceito; o "hoje" que decide o que está em aberto é o do servidor.
export function carregarConsistencia(
  mes: string,
  sinal?: AbortSignal,
): Promise<Consistencia> {
  return requisitar<Consistencia>(
    `/insights/consistencia?mes=${encodeURIComponent(mes)}`,
    { sinal },
  )
}
