import { describe, expect, it } from 'vitest'
import { hojeISO } from './datas'

describe('hojeISO', () => {
  it('formata a data local como AAAA-MM-DD', () => {
    expect(hojeISO(new Date(2026, 9, 21, 12, 0))).toBe('2026-10-21')
  })

  it('completa mês e dia com zero', () => {
    expect(hojeISO(new Date(2026, 0, 5, 8, 0))).toBe('2026-01-05')
  })

  it('usa o dia local mesmo à noite, quando o UTC já virou', () => {
    expect(hojeISO(new Date(2026, 9, 21, 23, 59))).toBe('2026-10-21')
  })

  it('usa o dia local logo após a meia-noite', () => {
    expect(hojeISO(new Date(2026, 9, 22, 0, 1))).toBe('2026-10-22')
  })

  it('trata a virada de ano', () => {
    expect(hojeISO(new Date(2026, 11, 31, 23, 30))).toBe('2026-12-31')
    expect(hojeISO(new Date(2027, 0, 1, 0, 30))).toBe('2027-01-01')
  })
})
