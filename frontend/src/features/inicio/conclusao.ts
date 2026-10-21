import type { QueryClient } from '@tanstack/react-query'
import type { Entrega, Inicio } from '../../api/tipos'
import { chavesInicio } from './chaves'

// Troca a entrega de mesmo id no Upcoming, sem alterar o original. Id
// desconhecido devolve o mesmo objeto.
export function substituirEntrega(inicio: Inicio, entrega: Entrega): Inicio {
  if (!inicio.upcoming.entregas.some((atual) => atual.id === entrega.id)) {
    return inicio
  }
  return {
    ...inicio,
    upcoming: {
      ...inicio.upcoming,
      entregas: inicio.upcoming.entregas.map((atual) =>
        atual.id === entrega.id ? entrega : atual,
      ),
    },
  }
}

// A entrega concluída (com a data de hoje) ou desfeita, sem alterar a original.
// Desfazer limpa também a data e o instante da conclusão, como o servidor.
export function comConclusao(
  entrega: Entrega,
  concluida: boolean,
  data: string,
): Entrega {
  const atualizada: Entrega = { ...entrega, concluida }
  if (concluida) {
    atualizada.concluidaData = data
  } else {
    delete atualizada.concluidaEm
    delete atualizada.concluidaData
  }
  return atualizada
}

// Resultado de concluir ou desfazer uma entrega no `Inicio`. `data` é o dia da
// conclusão (hoje). Só muda se o estado de fato muda.
export function aplicarConclusao(
  inicio: Inicio,
  id: number,
  concluida: boolean,
  data: string,
): Inicio {
  const alvo = inicio.upcoming.entregas.find((entrega) => entrega.id === id)
  if (alvo === undefined || alvo.concluida === concluida) return inicio

  return substituirEntrega(inicio, comConclusao(alvo, concluida, data))
}

export function lerEntregaDoCache(
  queryClient: QueryClient,
  dia: string,
  id: number,
): Entrega | undefined {
  return queryClient
    .getQueryData<Inicio>(chavesInicio.dia(dia))
    ?.upcoming.entregas.find((entrega) => entrega.id === id)
}

// Sem dados em cache (ainda carregando), não faz nada.
export function aplicarConclusaoNoCache(
  queryClient: QueryClient,
  dia: string,
  id: number,
  concluida: boolean,
): void {
  queryClient.setQueryData<Inicio>(chavesInicio.dia(dia), (atual) =>
    atual === undefined ? undefined : aplicarConclusao(atual, id, concluida, dia),
  )
}

// Devolve só aquela entrega ao estado em que estava, sem desfazer o que
// aconteceu com as outras enquanto a requisição estava em voo.
export function restaurarEntregaNoCache(
  queryClient: QueryClient,
  dia: string,
  entrega: Entrega,
): void {
  queryClient.setQueryData<Inicio>(chavesInicio.dia(dia), (atual) =>
    atual === undefined ? undefined : substituirEntrega(atual, entrega),
  )
}
