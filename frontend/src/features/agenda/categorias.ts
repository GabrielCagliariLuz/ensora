import type { CategoriaEvento } from '../../api/tipos'

interface OpcaoCategoria {
  valor: CategoriaEvento
  rotulo: string
  // Token de cor (styles/tokens.css). A lista de categorias é fixa e o banco a
  // confere; rótulos e cores moram aqui, no front.
  cor: string
}

export const CATEGORIAS: readonly OpcaoCategoria[] = [
  { valor: 'SAUDE', rotulo: 'Saúde', cor: 'var(--cat-saude)' },
  { valor: 'TRABALHO', rotulo: 'Trabalho', cor: 'var(--cat-trabalho)' },
  { valor: 'PESSOAL', rotulo: 'Pessoal', cor: 'var(--cat-pessoal)' },
  { valor: 'ESTUDO', rotulo: 'Estudo/Faculdade', cor: 'var(--cat-estudo)' },
  {
    valor: 'CELEBRACAO',
    rotulo: 'Lazer/Celebração',
    cor: 'var(--cat-celebracao)',
  },
]

export function rotuloCategoria(categoria: CategoriaEvento): string {
  return CATEGORIAS.find((opcao) => opcao.valor === categoria)?.rotulo ?? ''
}

// Cor da categoria, ou a neutra quando o evento não tem categoria.
export function corDaCategoria(categoria?: CategoriaEvento): string {
  return (
    CATEGORIAS.find((opcao) => opcao.valor === categoria)?.cor ??
    'var(--cat-neutra)'
  )
}
