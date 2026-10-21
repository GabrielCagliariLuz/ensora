import { requisitar } from './cliente'
import type { Evento, EventoRequest } from './tipos'

export function criarEvento(corpo: EventoRequest): Promise<Evento> {
  return requisitar<Evento>('/eventos', { metodo: 'POST', corpo })
}

// O PUT substitui o evento inteiro: o que não for enviado é limpo.
export function atualizarEvento(
  id: number,
  corpo: EventoRequest,
): Promise<Evento> {
  return requisitar<Evento>(`/eventos/${id}`, { metodo: 'PUT', corpo })
}

export function excluirEvento(id: number): Promise<void> {
  return requisitar<void>(`/eventos/${id}`, { metodo: 'DELETE' })
}
