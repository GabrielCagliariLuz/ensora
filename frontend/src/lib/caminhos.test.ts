import { describe, expect, it } from 'vitest'
import { calendarioNoDia } from './caminhos'

describe('calendarioNoDia', () => {
  it('leva só o dia quando não há registro', () => {
    expect(calendarioNoDia('2026-10-25')).toBe('/calendario?dia=2026-10-25')
  })

  it('leva também o tipo e o id do registro', () => {
    expect(calendarioNoDia('2026-10-25', { tipo: 'ENTREGA', id: 12 })).toBe(
      '/calendario?dia=2026-10-25&tipo=entrega&id=12',
    )
    expect(calendarioNoDia('2026-10-25', { tipo: 'EVENTO', id: 3 })).toBe(
      '/calendario?dia=2026-10-25&tipo=evento&id=3',
    )
  })
})
