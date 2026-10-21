import { requisitar } from './cliente'
import type { Calendario } from './tipos'

// `de` e `ate` (AAAA-MM-DD) são inclusivos; o servidor aceita até 62 dias entre
// as pontas. O front manda o intervalo da grade visível.
export function carregarCalendario(
  de: string,
  ate: string,
  sinal?: AbortSignal,
): Promise<Calendario> {
  const parametros = `de=${encodeURIComponent(de)}&ate=${encodeURIComponent(ate)}`
  return requisitar<Calendario>(`/calendario?${parametros}`, { sinal })
}
