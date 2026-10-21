import type { QueryClient } from '@tanstack/react-query'
import { chavesInicio } from '../inicio/chaves'

// Criar, editar ou excluir entrega e evento muda o Upcoming da Home. Fica num
// ponto só: quando o Calendar (#46) existir, a chave dele entra aqui.
// A promessa só resolve depois do refetch, então quem a devolve no onSuccess
// mantém a mutação pendente até a tela estar atualizada.
export async function invalidarDadosDeAgenda(
  queryClient: QueryClient,
): Promise<void> {
  await queryClient.invalidateQueries({ queryKey: chavesInicio.todos })
}
