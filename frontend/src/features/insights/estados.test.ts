import { describe, expect, it } from 'vitest'
import { varianteDaCelula } from './estados'

const HOJE = '2026-10-21'

describe('varianteDaCelula', () => {
  it('concluído, não realizado e em aberto mantêm a forma do estado', () => {
    expect(varianteDaCelula('CONCLUIDO', '2026-10-10', HOJE)).toBe('CONCLUIDO')
    expect(varianteDaCelula('NAO_REALIZADO', '2026-10-10', HOJE)).toBe(
      'NAO_REALIZADO',
    )
    expect(varianteDaCelula('EM_ABERTO', HOJE, HOJE)).toBe('EM_ABERTO')
  })

  it('descanso/futuro num dia que já passou é descanso', () => {
    expect(varianteDaCelula('DESCANSO_FUTURO', '2026-10-19', HOJE)).toBe(
      'DESCANSO',
    )
  })

  it('descanso/futuro em hoje é descanso (meta da semana já batida)', () => {
    expect(varianteDaCelula('DESCANSO_FUTURO', HOJE, HOJE)).toBe('DESCANSO')
  })

  it('descanso/futuro num dia que ainda vem é futuro', () => {
    expect(varianteDaCelula('DESCANSO_FUTURO', '2026-10-22', HOJE)).toBe(
      'FUTURO',
    )
  })
})
