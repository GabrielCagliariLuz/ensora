import { describe, expect, it } from 'vitest'
import { CATEGORIAS, corDaCategoria, rotuloCategoria } from './categorias'

describe('categorias', () => {
  it('cobre as cinco categorias do backend', () => {
    expect(CATEGORIAS.map((categoria) => categoria.valor)).toEqual([
      'SAUDE',
      'TRABALHO',
      'PESSOAL',
      'ESTUDO',
      'CELEBRACAO',
    ])
  })

  it('dá um rótulo a cada categoria', () => {
    expect(rotuloCategoria('SAUDE')).toBe('Saúde')
    expect(rotuloCategoria('ESTUDO')).toBe('Estudo/Faculdade')
    expect(rotuloCategoria('CELEBRACAO')).toBe('Lazer/Celebração')
  })

  it('dá uma cor diferente a cada categoria', () => {
    const cores = CATEGORIAS.map((categoria) => corDaCategoria(categoria.valor))

    expect(new Set(cores).size).toBe(CATEGORIAS.length)
    expect(cores).not.toContain('var(--cat-neutra)')
  })

  it('usa a cor neutra quando não há categoria', () => {
    expect(corDaCategoria(undefined)).toBe('var(--cat-neutra)')
    expect(corDaCategoria()).toBe('var(--cat-neutra)')
  })
})
