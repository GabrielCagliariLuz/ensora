import { requisitar } from './cliente'
import type { Entrega, EntregaRequest } from './tipos'

export function criarEntrega(corpo: EntregaRequest): Promise<Entrega> {
  return requisitar<Entrega>('/entregas', { metodo: 'POST', corpo })
}
