// Chaves do TanStack Query da Home. `dia(data)` é a leitura de um dia (a
// marcação otimista atualiza essa chave); `todos` é o prefixo, para invalidar
// qualquer dia de uma vez.
export const chavesInicio = {
  todos: ['inicio'] as const,
  dia: (data: string) => ['inicio', data] as const,
}
