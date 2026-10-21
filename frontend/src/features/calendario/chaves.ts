// Chaves do TanStack Query do Calendar. `intervalo(de, ate)` é a leitura de uma
// grade visível; `todos` é o prefixo, para invalidar ou atualizar qualquer
// intervalo em cache de uma vez (a conclusão de uma entrega aparece em todos
// os que a contêm).
export const chavesCalendario = {
  todos: ['calendario'] as const,
  intervalo: (de: string, ate: string) => ['calendario', de, ate] as const,
}
