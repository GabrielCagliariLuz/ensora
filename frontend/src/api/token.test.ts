import { afterEach, describe, expect, it, vi } from 'vitest'
import { definirToken, limparToken, obterToken } from './token'

describe('token em memória', () => {
  afterEach(() => {
    limparToken()
    vi.unstubAllGlobals()
  })

  it('começa sem token', () => {
    expect(obterToken()).toBeNull()
  })

  it('guarda e devolve o token definido', () => {
    definirToken('abc')

    expect(obterToken()).toBe('abc')
  })

  it('limpa o token', () => {
    definirToken('abc')
    limparToken()

    expect(obterToken()).toBeNull()
  })

  it('não grava em localStorage nem em sessionStorage', () => {
    const armazenamento = { setItem: vi.fn(), getItem: vi.fn() }
    vi.stubGlobal('localStorage', armazenamento)
    vi.stubGlobal('sessionStorage', armazenamento)

    definirToken('abc')
    obterToken()
    limparToken()

    expect(armazenamento.setItem).not.toHaveBeenCalled()
    expect(armazenamento.getItem).not.toHaveBeenCalled()
  })
})
