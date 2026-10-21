import { describe, expect, it } from 'vitest'
import { diaInicialDoMes, diaSelecionado } from './diaSelecionado'

const HOJE = '2026-10-21'

describe('diaInicialDoMes', () => {
  it('é hoje no mês atual', () => {
    expect(diaInicialDoMes('2026-10', HOJE)).toBe(HOJE)
  })

  it('é o dia 1 em outro mês', () => {
    expect(diaInicialDoMes('2026-11', HOJE)).toBe('2026-11-01')
    expect(diaInicialDoMes('2025-10', HOJE)).toBe('2025-10-01')
  })
})

describe('diaSelecionado', () => {
  it('mantém o dia escolhido enquanto está no mês visível', () => {
    expect(diaSelecionado('2026-10-09', '2026-10', HOJE)).toBe('2026-10-09')
  })

  it('sem escolha, usa o dia inicial do mês', () => {
    expect(diaSelecionado(null, '2026-10', HOJE)).toBe(HOJE)
  })

  it('ao trocar de mês, o dia escolhido deixa de valer', () => {
    expect(diaSelecionado('2026-10-09', '2026-11', HOJE)).toBe('2026-11-01')
  })
})
