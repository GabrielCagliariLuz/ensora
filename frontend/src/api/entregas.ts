import { requisitar } from './cliente'
import type { Entrega, EntregaRequest } from './tipos'

export function criarEntrega(corpo: EntregaRequest): Promise<Entrega> {
  return requisitar<Entrega>('/entregas', { metodo: 'POST', corpo })
}

// Concluir exige a data de hoje (AAAA-MM-DD, relógio do aparelho): o servidor
// guarda o dia da conclusão no fuso do usuário. Idempotente: concluir de novo
// mantém a conclusão original.
export function concluirEntrega(id: number, data: string): Promise<Entrega> {
  return requisitar<Entrega>(`/entregas/${id}`, {
    metodo: 'PATCH',
    corpo: { concluida: true, data },
  })
}

export function desfazerEntrega(id: number): Promise<Entrega> {
  return requisitar<Entrega>(`/entregas/${id}`, {
    metodo: 'PATCH',
    corpo: { concluida: false },
  })
}
