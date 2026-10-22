import type { QueryClient } from '@tanstack/react-query'
import { chavesInicio } from '../inicio/chaves'
import { chavesInsights } from '../insights/chaves'
import { chavesHabitos } from './chaves'

// Criar, editar e excluir hábito mudam a lista de hábitos, o que a Home mostra e
// a grade dos Insights. Invalidar os dois prefixos aqui, num ponto só, evita uma
// mutação esquecer um deles (e a próxima tela que dependa de hábitos entra
// aqui também). O prefixo ['inicio'] cobre qualquer dia em cache; só as
// consultas ativas (a Home aberta) são buscadas de novo.
// A promessa só resolve depois do refetch, então quem a devolve no onSuccess
// mantém a mutação pendente até a tela estar atualizada.
export async function invalidarDadosDeHabitos(
  queryClient: QueryClient,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: chavesHabitos.todos }),
    queryClient.invalidateQueries({ queryKey: chavesInicio.todos }),
    queryClient.invalidateQueries({ queryKey: chavesInsights.todos }),
  ])
}
