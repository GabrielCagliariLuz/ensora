// Chaves do TanStack Query dos Insights. `consistencia(mes)` é a leitura de um
// mês ('AAAA-MM'); `todos` é o prefixo, para invalidar qualquer mês em cache
// de uma vez (marcar um hábito muda a célula de hoje).
export const chavesInsights = {
  todos: ['insights'] as const,
  consistencia: (mes: string) => ['insights', 'consistencia', mes] as const,
}
