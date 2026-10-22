import { describe, expect, it } from 'vitest'
import { deveFechar } from './useArrastarParaFechar'

describe('deveFechar', () => {
  it('não fecha com um arrasto curto e lento', () => {
    expect(deveFechar(10, 0.05)).toBe(false)
    expect(deveFechar(60, 0.1)).toBe(false)
  })

  it('fecha quando passa da distância, mesmo devagar', () => {
    expect(deveFechar(100, 0.05)).toBe(true)
    expect(deveFechar(220, 0.1)).toBe(true)
  })

  it('fecha com um arrasto rápido que já desceu um pouco', () => {
    expect(deveFechar(40, 0.8)).toBe(true)
  })

  it('não fecha por um toque trêmulo, ainda que rápido', () => {
    expect(deveFechar(10, 2)).toBe(false)
    expect(deveFechar(0, 5)).toBe(false)
  })
})
