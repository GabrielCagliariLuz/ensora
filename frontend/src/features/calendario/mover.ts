import type { QueryClient } from '@tanstack/react-query'
import type { Calendario } from '../../api/tipos'
import type { Registro } from './agenda'
import { chavesCalendario } from './chaves'

export function idDoRegistro(registro: Registro): number {
  return registro.tipo === 'EVENTO' ? registro.evento.id : registro.entrega.id
}

// O dia em que o registro aparece: a data do evento ou o prazo da entrega.
export function dataDoRegistro(registro: Registro): string {
  return registro.tipo === 'EVENTO'
    ? registro.evento.data
    : registro.entrega.prazoData
}

// O mesmo registro, em outro dia: a data do evento ou o prazo da entrega.
export function comNovaData(registro: Registro, data: string): Registro {
  return registro.tipo === 'EVENTO'
    ? { tipo: 'EVENTO', evento: { ...registro.evento, data } }
    : { tipo: 'ENTREGA', entrega: { ...registro.entrega, prazoData: data } }
}

// Troca o evento ou a entrega de mesmo id, sem alterar o original. Como as
// listas são planas, mudar a data já muda o dia em que ele aparece. Id
// desconhecido devolve o mesmo objeto.
export function substituirRegistro(
  calendario: Calendario,
  registro: Registro,
): Calendario {
  if (registro.tipo === 'EVENTO') {
    const { evento } = registro
    if (!calendario.eventos.some((atual) => atual.id === evento.id)) {
      return calendario
    }
    return {
      ...calendario,
      eventos: calendario.eventos.map((atual) =>
        atual.id === evento.id ? evento : atual,
      ),
    }
  }

  const { entrega } = registro
  if (!calendario.entregas.some((atual) => atual.id === entrega.id)) {
    return calendario
  }
  return {
    ...calendario,
    entregas: calendario.entregas.map((atual) =>
      atual.id === entrega.id ? entrega : atual,
    ),
  }
}

// O registro como está em qualquer grade em cache, ou undefined.
export function lerRegistroDoCache(
  queryClient: QueryClient,
  tipo: Registro['tipo'],
  id: number,
): Registro | undefined {
  for (const [, calendario] of queryClient.getQueriesData<Calendario>({
    queryKey: chavesCalendario.todos,
  })) {
    if (calendario === undefined) continue

    if (tipo === 'EVENTO') {
      const evento = calendario.eventos.find((atual) => atual.id === id)
      if (evento) return { tipo, evento }
    } else {
      const entrega = calendario.entregas.find((atual) => atual.id === id)
      if (entrega) return { tipo, entrega }
    }
  }
  return undefined
}

// Aplica o registro em todas as grades em cache que o contêm. Serve para mover
// e, com o registro guardado antes, para desfazer só aquele movimento sem
// reverter o que aconteceu com os outros enquanto a requisição estava em voo.
export function substituirRegistroNoCache(
  queryClient: QueryClient,
  registro: Registro,
): void {
  queryClient.setQueriesData<Calendario>(
    { queryKey: chavesCalendario.todos },
    (atual) =>
      atual === undefined ? undefined : substituirRegistro(atual, registro),
  )
}
