import { describe, expect, it } from 'vitest'
import { gradeDoMes, intervaloDaGrade, mesDoDia, somarMeses } from './grade'

describe('mesDoDia', () => {
  it('devolve o mês de uma data', () => {
    expect(mesDoDia('2026-10-21')).toBe('2026-10')
    expect(mesDoDia('2027-01-01')).toBe('2027-01')
  })
})

describe('somarMeses', () => {
  it('avança e volta meses', () => {
    expect(somarMeses('2026-10', 1)).toBe('2026-11')
    expect(somarMeses('2026-10', -1)).toBe('2026-09')
    expect(somarMeses('2026-10', 0)).toBe('2026-10')
  })

  it('atravessa o ano', () => {
    expect(somarMeses('2026-12', 1)).toBe('2027-01')
    expect(somarMeses('2026-01', -1)).toBe('2025-12')
  })

  it('soma vários meses de uma vez', () => {
    expect(somarMeses('2026-10', 14)).toBe('2027-12')
    expect(somarMeses('2026-10', -10)).toBe('2025-12')
  })
})

describe('gradeDoMes', () => {
  it('outubro de 2026 tem 5 semanas, de 27/09 a 31/10', () => {
    const grade = gradeDoMes('2026-10')

    expect(grade).toHaveLength(35)
    expect(grade[0]).toBe('2026-09-27')
    expect(grade[34]).toBe('2026-10-31')
  })

  it('começa sempre num domingo e termina num sábado', () => {
    for (const mes of ['2026-01', '2026-02', '2026-03', '2026-10', '2027-02']) {
      const grade = gradeDoMes(mes)

      expect(new Date(`${grade[0]}T12:00:00`).getDay()).toBe(0)
      expect(new Date(`${grade[grade.length - 1]}T12:00:00`).getDay()).toBe(6)
    }
  })

  it('usa 6 semanas quando o mês precisa', () => {
    // Novembro de 2026 começa num domingo e termina numa segunda: 5 linhas.
    expect(gradeDoMes('2026-11')).toHaveLength(35)
    // Agosto de 2026 começa num sábado e tem 31 dias: 6 linhas.
    const agosto = gradeDoMes('2026-08')

    expect(agosto).toHaveLength(42)
    expect(agosto[0]).toBe('2026-07-26')
    expect(agosto[41]).toBe('2026-09-05')
  })

  it('fevereiro que começa no domingo cabe em 4 semanas', () => {
    // Fevereiro de 2026 começa num domingo e tem 28 dias.
    const fevereiro = gradeDoMes('2026-02')

    expect(fevereiro).toHaveLength(28)
    expect(fevereiro[0]).toBe('2026-02-01')
    expect(fevereiro[27]).toBe('2026-02-28')
  })

  it('tem 7 dias por linha, em dias consecutivos e sem repetir', () => {
    const grade = gradeDoMes('2027-03')

    expect(grade.length % 7).toBe(0)
    expect(new Set(grade).size).toBe(grade.length)
    expect(grade).toContain('2027-03-01')
    expect(grade).toContain('2027-03-31')
  })

  it('inclui todos os dias do mês', () => {
    const grade = gradeDoMes('2026-10')
    const doMes = grade.filter((dia) => mesDoDia(dia) === '2026-10')

    expect(doMes).toHaveLength(31)
  })
})

describe('intervaloDaGrade', () => {
  it('devolve o primeiro e o último dia da grade', () => {
    expect(intervaloDaGrade('2026-10')).toEqual({
      de: '2026-09-27',
      ate: '2026-10-31',
    })
  })

  it('nunca passa dos 62 dias aceitos pelo servidor', () => {
    expect(gradeDoMes('2026-08').length).toBeLessThanOrEqual(62)
  })
})
