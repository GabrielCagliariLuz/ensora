import { requisitar } from './cliente'
import type { Evento, EventoRequest } from './tipos'

export function criarEvento(corpo: EventoRequest): Promise<Evento> {
  return requisitar<Evento>('/eventos', { metodo: 'POST', corpo })
}
