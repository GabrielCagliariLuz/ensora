import type { QueryClient } from '@tanstack/react-query'
import { chavesCalendario } from '../calendario/chaves'
import { chavesInicio } from '../inicio/chaves'

// Criar, editar, excluir ou concluir entrega e evento muda o Upcoming da Home e
// o Calendar. Os dois prefixos são invalidados aqui, num ponto só, para as duas
// telas ficarem coerentes; só as consultas ativas são buscadas de novo.
// A promessa só resolve depois do refetch, então quem a devolve no onSuccess
// mantém a mutação pendente até a tela estar atualizada.
export async function invalidarDadosDeAgenda(
  queryClient: QueryClient,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: chavesInicio.todos }),
    queryClient.invalidateQueries({ queryKey: chavesCalendario.todos }),
  ])
}
