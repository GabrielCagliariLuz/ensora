import { useMutation, useQueryClient } from '@tanstack/react-query'
import { concluirEntrega, desfazerEntrega } from '../../api/entregas'
import { chavesInicio } from './chaves'
import {
  aplicarConclusaoNoCache,
  lerEntregaDoCache,
  restaurarEntregaNoCache,
} from './conclusao'

export interface ConclusaoEntrega {
  id: number
  titulo: string
  // Estado pedido: true conclui, false desfaz.
  concluida: boolean
  // Dia de hoje (AAAA-MM-DD) no momento do clique: é a chave do cache da Home
  // e a data da conclusão enviada ao servidor.
  data: string
}

const CHAVE_MUTACAO = ['concluir-entrega'] as const

// Atualização otimista, no mesmo padrão de useMarcarHabito: a entrega muda no
// clique e a API roda em seguida.
export function useConcluirEntrega(
  aoFalhar?: (erro: unknown, conclusao: ConclusaoEntrega) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationKey: CHAVE_MUTACAO,
    // Uma de cada vez, na ordem do clique, para um "concluir" e um "desfazer"
    // da mesma entrega nunca chegarem invertidos ao servidor. A tela continua
    // instantânea: o onMutate roda na hora, mesmo com a requisição na fila.
    scope: { id: 'conclusao-entrega' },
    mutationFn: ({ id, concluida, data }: ConclusaoEntrega) =>
      concluida ? concluirEntrega(id, data) : desfazerEntrega(id),
    onMutate: async ({ id, concluida, data }) => {
      // Uma busca em voo traria o estado antigo por cima da conclusão.
      await queryClient.cancelQueries({ queryKey: chavesInicio.dia(data) })
      // Guarda a entrega como estava. Reverter com a operação inversa não basta:
      // desfazer uma entrega concluída ontem e voltar atrás a marcaria como
      // concluída hoje.
      const anterior = lerEntregaDoCache(queryClient, data, id)
      aplicarConclusaoNoCache(queryClient, data, id, concluida)
      return { anterior }
    },
    onError: (erro, conclusao, contexto) => {
      if (contexto?.anterior) {
        restaurarEntregaNoCache(queryClient, conclusao.data, contexto.anterior)
      }
      aoFalhar?.(erro, conclusao)
    },
    onSettled: (_dados, _erro, { data }) => {
      // Só a última conclusão em andamento reconcilia com o servidor.
      if (queryClient.isMutating({ mutationKey: CHAVE_MUTACAO }) === 1) {
        return queryClient.invalidateQueries({
          queryKey: chavesInicio.dia(data),
        })
      }
    },
  })
}
