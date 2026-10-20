import { requisitar } from './cliente'
import type { Habito, HabitoRequest } from './tipos'

export function listarHabitos(sinal?: AbortSignal): Promise<Habito[]> {
  return requisitar<Habito[]>('/habitos', { sinal })
}

export function criarHabito(corpo: HabitoRequest): Promise<Habito> {
  return requisitar<Habito>('/habitos', { metodo: 'POST', corpo })
}

export function editarHabito(id: number, corpo: HabitoRequest): Promise<Habito> {
  return requisitar<Habito>(`/habitos/${id}`, { metodo: 'PUT', corpo })
}

// Marcar e desmarcar são idempotentes: repetir a operação também responde 204.
// `data` é o dia a que o registro se refere (AAAA-MM-DD).
export function marcarHabito(id: number, data: string): Promise<void> {
  return requisitar<void>(`/habitos/${id}/registros/${data}`, { metodo: 'PUT' })
}

export function desmarcarHabito(id: number, data: string): Promise<void> {
  return requisitar<void>(`/habitos/${id}/registros/${data}`, {
    metodo: 'DELETE',
  })
}

// Exclusão lógica no servidor: responde 204 e o hábito some das telas.
export function excluirHabito(id: number): Promise<void> {
  return requisitar<void>(`/habitos/${id}`, { metodo: 'DELETE' })
}
