import { requisitar } from './cliente'
import type { Inicio } from './tipos'

// A data é o dia de hoje do aparelho (AAAA-MM-DD): o servidor não decide
// "hoje", só confere que ela não está muito longe do relógio dele.
export function carregarInicio(data: string, sinal?: AbortSignal): Promise<Inicio> {
  return requisitar<Inicio>(`/inicio?data=${encodeURIComponent(data)}`, { sinal })
}
