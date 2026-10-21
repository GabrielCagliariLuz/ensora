import { describe, expect, it } from 'vitest'
import type { Registro } from './agenda'
import { pontosDoDia, PONTOS_MAXIMOS } from './pontos'

const AGORA = new Date(2026, 9, 21, 12, 0, 0)

function evento(id: number, categoria?: 'SAUDE' | 'ESTUDO'): Registro {
  return {
    tipo: 'EVENTO',
    evento: { id, titulo: 'E', data: '2026-10-21', inicio: '09:00:00', categoria },
  }
}

describe('pontosDoDia', () => {
  it('dia sem registros não tem pontos', () => {
    expect(pontosDoDia([], AGORA)).toEqual([])
  })

  it('um ponto por registro, na cor dele', () => {
    const entrega: Registro = {
      tipo: 'ENTREGA',
      entrega: { id: 1, titulo: 'T', prazoData: '2026-10-21', concluida: false },
    }

    expect(pontosDoDia([evento(1, 'SAUDE'), entrega], AGORA)).toEqual([
      'var(--cat-saude)',
      'var(--ember)',
    ])
  })

  it('limita a quantidade de pontos', () => {
    const muitos = [1, 2, 3, 4, 5].map((id) => evento(id))

    expect(pontosDoDia(muitos, AGORA)).toHaveLength(PONTOS_MAXIMOS)
  })
})
