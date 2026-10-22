// Chaves do TanStack Query dos Insights. `consistencia(mes)` é a leitura de um
// mês ('AAAA-MM') e `ritmo(de, ate)`, a de um intervalo; `todos` é o prefixo, para
// invalidar tudo de uma vez (marcar um hábito muda a célula de hoje e o ritmo).
export const chavesInsights = {
  todos: ['insights'] as const,
  consistencia: (mes: string) => ['insights', 'consistencia', mes] as const,
  ritmo: (de: string, ate: string) => ['insights', 'ritmo', de, ate] as const,
  // Mês que ainda não começou: não há consulta, só uma chave para a tela.
  ritmoFuturo: (mes: string) => ['insights', 'ritmo', mes, 'futuro'] as const,
}
