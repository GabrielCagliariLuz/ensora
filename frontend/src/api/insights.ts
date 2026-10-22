import { requisitar } from './cliente'
import type { Consistencia, Ritmo } from './tipos'

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

// O ritmo de `de` a `ate` (AAAA-MM-DD, as duas pontas inclusivas, no máximo 31
// dias). No mês em andamento o front manda `ate` = hoje: o servidor conta todo
// dia do intervalo como já decorrido.
export function carregarRitmo(
  de: string,
  ate: string,
  sinal?: AbortSignal,
): Promise<Ritmo> {
  return requisitar<Ritmo>(
    `/insights/ritmo?de=${encodeURIComponent(de)}&ate=${encodeURIComponent(ate)}`,
    { sinal },
  )
}
