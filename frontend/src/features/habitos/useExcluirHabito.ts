import { useMutation, useQueryClient } from '@tanstack/react-query'
import { excluirHabito } from '../../api/habitos'
import { ErroApi } from '../../api/erro'
import { invalidarDadosDeHabitos } from './invalidarHabitos'

export function useExcluirHabito() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: number) => {
      try {
        await excluirHabito(id)
      } catch (erro) {
        // 404: o hábito já não existe (excluído em outra aba, por exemplo).
        // O resultado é o esperado, então segue como sucesso e a lista
        // é atualizada.
        if (erro instanceof ErroApi && erro.status === 404) return
        throw erro
      }
    },
    // A mutação fica pendente até o refetch terminar, então o hábito já saiu
    // da lista e da Home quando o modal fecha.
    onSuccess: () => invalidarDadosDeHabitos(queryClient),
  })
}
