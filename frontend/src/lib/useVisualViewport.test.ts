import { describe, expect, it } from 'vitest'
import { espacoDoTeclado } from './useVisualViewport'

describe('espacoDoTeclado', () => {
  it('é zero quando a área visível é a janela inteira', () => {
    expect(espacoDoTeclado(800, 800, 0)).toBe(0)
  })

  it('é a altura que o teclado tira da área visível', () => {
    expect(espacoDoTeclado(800, 400, 0)).toBe(400)
  })

  it('desconta o quanto a página foi empurrada para cima', () => {
    expect(espacoDoTeclado(800, 400, 50)).toBe(350)
  })

  it('nunca é negativo', () => {
    expect(espacoDoTeclado(800, 900, 0)).toBe(0)
  })
})
