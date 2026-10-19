import { requisitar } from './cliente'
import type { Habito, HabitoRequest } from './tipos'

export function criarHabito(corpo: HabitoRequest): Promise<Habito> {
  return requisitar<Habito>('/habitos', { metodo: 'POST', corpo })
}

export function editarHabito(id: number, corpo: HabitoRequest): Promise<Habito> {
  return requisitar<Habito>(`/habitos/${id}`, { metodo: 'PUT', corpo })
}
