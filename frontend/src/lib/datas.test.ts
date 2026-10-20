import { describe, expect, it } from 'vitest'
import { hojeISO, rotuloDia } from './datas'

describe('rotuloDia', () => {
  it('mostra dia e mês abreviado em maiúsculas', () => {
    expect(rotuloDia('2026-09-26')).toBe('26 SET')
    expect(rotuloDia('2026-10-21')).toBe('21 OUT')
  })

  it('mantém o zero do dia', () => {
    expect(rotuloDia('2026-01-05')).toBe('05 JAN')
  })

  it('cobre os doze meses', () => {
    const meses = Array.from({ length: 12 }, (_, i) =>
      rotuloDia(`2026-${String(i + 1).padStart(2, '0')}-10`),
    )

    expect(meses).toEqual([
      '10 JAN',
      '10 FEV',
      '10 MAR',
      '10 ABR',
      '10 MAI',
      '10 JUN',
      '10 JUL',
      '10 AGO',
      '10 SET',
      '10 OUT',
      '10 NOV',
      '10 DEZ',
    ])
  })

  it('devolve o texto original quando a data é inválida', () => {
    expect(rotuloDia('26/09/2026')).toBe('26/09/2026')
    expect(rotuloDia('2026-13-01')).toBe('2026-13-01')
    expect(rotuloDia('')).toBe('')
  })
})

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
