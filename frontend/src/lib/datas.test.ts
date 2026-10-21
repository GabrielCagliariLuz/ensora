import { describe, expect, it } from 'vitest'
import {
  dataCurta,
  diaDaSemana,
  diaDoMes,
  diasEntre,
  hojeISO,
  rotuloDia,
} from './datas'

describe('diasEntre', () => {
  it('conta os dias de calendário', () => {
    expect(diasEntre('2026-10-21', '2026-10-24')).toBe(3)
    expect(diasEntre('2026-10-21', '2026-10-21')).toBe(0)
  })

  it('é negativo quando a data de destino é anterior', () => {
    expect(diasEntre('2026-10-21', '2026-10-20')).toBe(-1)
    expect(diasEntre('2026-10-21', '2026-10-16')).toBe(-5)
  })

  it('atravessa meses e anos', () => {
    expect(diasEntre('2026-10-30', '2026-11-02')).toBe(3)
    expect(diasEntre('2026-12-31', '2027-01-01')).toBe(1)
  })

  it('não erra na virada do horário de verão', () => {
    // Em fusos com horário de verão, esse intervalo tem 23 ou 25 horas.
    expect(diasEntre('2026-03-28', '2026-03-30')).toBe(2)
    expect(diasEntre('2026-10-24', '2026-10-26')).toBe(2)
    expect(diasEntre('2026-11-01', '2026-11-03')).toBe(2)
  })
})

describe('dataCurta', () => {
  it('mostra dia e mês abreviado em minúsculas', () => {
    expect(dataCurta('2026-10-04')).toBe('04 out')
    expect(dataCurta('2026-09-29')).toBe('29 set')
  })

  it('devolve o texto original quando a data é inválida', () => {
    expect(dataCurta('04/10/2026')).toBe('04/10/2026')
  })
})

describe('diaDoMes', () => {
  it('devolve o dia com dois dígitos', () => {
    expect(diaDoMes('2026-10-04')).toBe('04')
    expect(diaDoMes('2026-10-29')).toBe('29')
  })

  it('devolve vazio para data inválida', () => {
    expect(diaDoMes('hoje')).toBe('')
  })
})

describe('diaDaSemana', () => {
  it('abrevia o dia da semana em maiúsculas', () => {
    expect(diaDaSemana('2026-10-23')).toBe('SEX')
    expect(diaDaSemana('2026-10-24')).toBe('SAB')
    expect(diaDaSemana('2026-10-25')).toBe('DOM')
    expect(diaDaSemana('2026-10-26')).toBe('SEG')
    expect(diaDaSemana('2026-10-27')).toBe('TER')
    expect(diaDaSemana('2026-10-28')).toBe('QUA')
    expect(diaDaSemana('2026-10-29')).toBe('QUI')
  })

  it('devolve vazio para data inválida', () => {
    expect(diaDaSemana('hoje')).toBe('')
  })
})

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
