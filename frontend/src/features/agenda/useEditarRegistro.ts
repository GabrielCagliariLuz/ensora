import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atualizarEntrega, excluirEntrega } from '../../api/entregas'
import { atualizarEvento, excluirEvento } from '../../api/eventos'
import type {
  Entrega,
  EntregaRequest,
  Evento,
  EventoRequest,
} from '../../api/tipos'
import type { Registro } from '../calendario/agenda'
import { invalidarDadosDeAgenda } from './invalidarAgenda'
import { useCriarEntrega } from './useCriarEntrega'
import { useCriarEvento } from './useCriarEvento'

// Editar e excluir esperam o servidor (o sheet e o modal ficam abertos até
// terminar) e depois atualizam a Home e o Calendar.

function useEditarEvento(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (corpo: EventoRequest) => atualizarEvento(id, corpo),
    onSuccess: () => invalidarDadosDeAgenda(queryClient),
  })
}

function useEditarEntrega(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (corpo: EntregaRequest) => atualizarEntrega(id, corpo),
    onSuccess: () => invalidarDadosDeAgenda(queryClient),
  })
}

// O mesmo formulário cria (sem `existente`) ou edita (com ele): devolve a
// mutação que cabe, com a mesma forma nos dois casos. Os dois hooks sempre
// rodam, e só um é usado.
export function useSalvarEvento(existente?: Evento) {
  const criar = useCriarEvento()
  const editar = useEditarEvento(existente?.id ?? 0)
  return existente ? editar : criar
}

export function useSalvarEntrega(existente?: Entrega) {
  const criar = useCriarEntrega()
  const editar = useEditarEntrega(existente?.id ?? 0)
  return existente ? editar : criar
}

export function useExcluirRegistro() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (registro: Registro): Promise<void> =>
      registro.tipo === 'EVENTO'
        ? excluirEvento(registro.evento.id)
        : excluirEntrega(registro.entrega.id),
    onSuccess: () => invalidarDadosDeAgenda(queryClient),
  })
}
