import { describe, expect, it } from 'vitest'
import type { Entrega } from '../../api/tipos'
import type { Registro } from './agenda'
import { corDoRegistro, entregaUrgente } from './cores'

const AGORA = new Date(2026, 9, 21, 12, 0, 0)

function entrega(parcial: Partial<Entrega> = {}): Entrega {
  return {
    id: 1,
    titulo: 'Trabalho',
    prazoData: '2026-10-30',
    concluida: false,
    ...parcial,
  }
}

describe('entregaUrgente', () => {
  it('é urgente quando atrasada, hoje ou amanhã', () => {
    expect(entregaUrgente(entrega({ prazoData: '2026-10-20' }), AGORA)).toBe(true)
    expect(entregaUrgente(entrega({ prazoData: '2026-10-21' }), AGORA)).toBe(true)
    expect(entregaUrgente(entrega({ prazoData: '2026-10-22' }), AGORA)).toBe(true)
  })

  it('não é urgente com tempo de sobra', () => {
    expect(entregaUrgente(entrega(), AGORA)).toBe(false)
  })

  it('concluída nunca é urgente', () => {
    const concluida = entrega({ prazoData: '2026-10-20', concluida: true })

    expect(entregaUrgente(concluida, AGORA)).toBe(false)
  })
})

describe('corDoRegistro', () => {
  it('evento usa a cor da categoria, ou a neutra sem categoria', () => {
    const comCategoria: Registro = {
      tipo: 'EVENTO',
      evento: {
        id: 1,
        titulo: 'Consulta',
        data: '2026-10-21',
        inicio: '09:00:00',
        categoria: 'SAUDE',
      },
    }
    const semCategoria: Registro = {
      tipo: 'EVENTO',
      evento: { id: 2, titulo: 'X', data: '2026-10-21', inicio: '09:00:00' },
    }

    expect(corDoRegistro(comCategoria, AGORA)).toBe('var(--cat-saude)')
    expect(corDoRegistro(semCategoria, AGORA)).toBe('var(--cat-neutra)')
  })

  it('entrega urgente é ember e a comum é ink', () => {
    const urgente: Registro = {
      tipo: 'ENTREGA',
      entrega: entrega({ prazoData: '2026-10-21' }),
    }
    const comum: Registro = { tipo: 'ENTREGA', entrega: entrega() }

    expect(corDoRegistro(urgente, AGORA)).toBe('var(--ember)')
    expect(corDoRegistro(comum, AGORA)).toBe('var(--ink)')
  })
})
