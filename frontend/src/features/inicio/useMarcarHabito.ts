import { useMutation, useQueryClient } from '@tanstack/react-query'
import { desmarcarHabito, marcarHabito } from '../../api/habitos'
import { chavesInsights } from '../insights/chaves'
import { chavesInicio } from './chaves'
import { aplicarMarcacaoNoCache } from './marcacao'

export interface MarcacaoHabito {
  id: number
  nome: string
  // Estado pedido: true marca (PUT), false desmarca (DELETE).
  feito: boolean
  // Dia da marcação (AAAA-MM-DD), no momento do clique: a Home pode virar de
  // dia enquanto a requisição está em voo.
  data: string
}

const CHAVE_MUTACAO = ['marcar-habito'] as const

// Atualização otimista: o cartão muda no clique, e a API roda em seguida.
export function useMarcarHabito(
  aoFalhar?: (erro: unknown, marcacao: MarcacaoHabito) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationKey: CHAVE_MUTACAO,
    // Marcações rodam uma de cada vez, na ordem do clique, para um PUT e um
    // DELETE do mesmo hábito nunca chegarem invertidos ao servidor. A tela
    // continua instantânea: o onMutate roda na hora, mesmo com a requisição na
    // fila. O escopo é único (e não por hábito) porque ele é fixo no hook.
    scope: { id: 'registro-habito' },
    mutationFn: ({ id, feito, data }: MarcacaoHabito) =>
      feito ? marcarHabito(id, data) : desmarcarHabito(id, data),
    onMutate: async ({ id, feito, data }) => {
      // Uma busca em voo traria o estado antigo por cima da marcação.
      await queryClient.cancelQueries({ queryKey: chavesInicio.dia(data) })
      aplicarMarcacaoNoCache(queryClient, data, id, feito)
    },
    onError: (erro, marcacao) => {
      // Reverte só este hábito: um snapshot da lista inteira desfaria
      // marcações de outros hábitos ainda em voo.
      aplicarMarcacaoNoCache(
        queryClient,
        marcacao.data,
        marcacao.id,
        !marcacao.feito,
      )
      aoFalhar?.(erro, marcacao)
    },
    onSettled: (_dados, _erro, { data }) => {
      // Só a última marcação em andamento reconcilia com o servidor: refetch no
      // meio de uma sequência de cliques faria o contador piscar.
      if (queryClient.isMutating({ mutationKey: CHAVE_MUTACAO }) === 1) {
        // A grade dos Insights também muda (a célula de hoje).
        return Promise.all([
          queryClient.invalidateQueries({ queryKey: chavesInicio.dia(data) }),
          queryClient.invalidateQueries({ queryKey: chavesInsights.todos }),
        ])
      }
    },
  })
}
