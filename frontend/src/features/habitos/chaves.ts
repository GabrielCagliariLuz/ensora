// Chaves do TanStack Query para hábitos. A lista (#18) e as mutações
// compartilham o prefixo, então invalidar `todos` atualiza tudo de hábitos.
export const chavesHabitos = {
  todos: ['habitos'] as const,
}
