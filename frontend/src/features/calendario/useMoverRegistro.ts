import { useMutation, useQueryClient } from '@tanstack/react-query'
import { atualizarEntrega } from '../../api/entregas'
import { atualizarEvento } from '../../api/eventos'
import { invalidarDadosDeAgenda } from '../agenda/invalidarAgenda'
import { requisicaoDaEntrega, requisicaoDoEvento } from '../agenda/regras'
import type { Registro } from './agenda'
import { chavesCalendario } from './chaves'
import {
  comNovaData,
  idDoRegistro,
  lerRegistroDoCache,
  substituirRegistroNoCache,
} from './mover'

export interface MovimentoRegistro {
  registro: Registro
  // Dia de destino (AAAA-MM-DD).
  para: string
}

const CHAVE_MUTACAO = ['mover-registro'] as const

// Arrastar um evento ou uma entrega para outro dia. Otimista, como a conclusão:
// a etiqueta muda de dia no soltar e o PUT roda em seguida; se falhar, só ela
// volta ao dia de antes.
export function useMoverRegistro(
  aoFalhar?: (erro: unknown, movimento: MovimentoRegistro) => void,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationKey: CHAVE_MUTACAO,
    // Um movimento por vez, na ordem em que foram feitos.
    scope: { id: 'mover-registro' },
    mutationFn: async ({ registro, para }: MovimentoRegistro): Promise<void> => {
      if (registro.tipo === 'EVENTO') {
        await atualizarEvento(
          registro.evento.id,
          requisicaoDoEvento(registro.evento, { data: para }),
        )
      } else {
        await atualizarEntrega(
          registro.entrega.id,
          requisicaoDaEntrega(registro.entrega, { prazoData: para }),
        )
      }
    },
    onMutate: async ({ registro, para }) => {
      // Uma busca em voo traria o dia antigo por cima do movimento.
      await queryClient.cancelQueries({ queryKey: chavesCalendario.todos })
      // O estado de antes vem do cache, não do argumento: dois movimentos
      // seguidos do mesmo registro devolvem cada um ao dia certo.
      const anterior = lerRegistroDoCache(
        queryClient,
        registro.tipo,
        idDoRegistro(registro),
      )
      substituirRegistroNoCache(queryClient, comNovaData(registro, para))
      return { anterior }
    },
    onError: (erro, movimento, contexto) => {
      if (contexto?.anterior) {
        substituirRegistroNoCache(queryClient, contexto.anterior)
      }
      aoFalhar?.(erro, movimento)
    },
    onSettled: () => {
      // Só o último movimento em andamento reconcilia com o servidor.
      if (queryClient.isMutating({ mutationKey: CHAVE_MUTACAO }) === 1) {
        return invalidarDadosDeAgenda(queryClient)
      }
    },
  })
}
