import { describe, expect, it } from 'vitest'
import {
  diasDaSemana,
  ehFimDeSemana,
  ehSegunda,
  diasDoMes,
  inicialDoDia,
  mesesDaSemana,
  numeroDaSemana,
  rotuloDaSemana,
  segundaDaSemana,
} from './semana'

describe('diasDoMes', () => {
  it('vai do dia 1 ao último dia do mês', () => {
    const setembro = diasDoMes('2026-09')

    expect(setembro).toHaveLength(30)
    expect(setembro[0]).toBe('2026-09-01')
    expect(setembro[29]).toBe('2026-09-30')
  })

  it('respeita meses de 31 dias e fevereiro', () => {
    expect(diasDoMes('2026-10')).toHaveLength(31)
    expect(diasDoMes('2026-02')).toHaveLength(28)
    expect(diasDoMes('2028-02')).toHaveLength(29)
  })
})

describe('inicialDoDia', () => {
  it('é a inicial do dia da semana', () => {
    expect(inicialDoDia('2026-09-21')).toBe('S') // segunda
    expect(inicialDoDia('2026-09-22')).toBe('T') // terça
    expect(inicialDoDia('2026-09-23')).toBe('Q') // quarta
    expect(inicialDoDia('2026-09-27')).toBe('D') // domingo
  })
})

describe('ehSegunda e ehFimDeSemana', () => {
  it('reconhece a segunda-feira', () => {
    expect(ehSegunda('2026-09-21')).toBe(true)
    expect(ehSegunda('2026-09-22')).toBe(false)
    expect(ehSegunda('2026-09-27')).toBe(false)
  })

  it('sábado e domingo são fim de semana', () => {
    expect(ehFimDeSemana('2026-09-26')).toBe(true)
    expect(ehFimDeSemana('2026-09-27')).toBe(true)
    expect(ehFimDeSemana('2026-09-25')).toBe(false)
    expect(ehFimDeSemana('2026-09-21')).toBe(false)
  })
})

describe('segundaDaSemana', () => {
  it('devolve a própria segunda-feira', () => {
    expect(segundaDaSemana('2026-09-21')).toBe('2026-09-21')
  })

  it('volta até a segunda a partir de qualquer dia da semana', () => {
    expect(segundaDaSemana('2026-09-23')).toBe('2026-09-21')
    expect(segundaDaSemana('2026-09-26')).toBe('2026-09-21')
  })

  it('o domingo pertence à semana que termina nele', () => {
    expect(segundaDaSemana('2026-09-27')).toBe('2026-09-21')
  })

  it('atravessa a virada do mês e do ano', () => {
    expect(segundaDaSemana('2026-10-02')).toBe('2026-09-28')
    expect(segundaDaSemana('2027-01-01')).toBe('2026-12-28')
  })
})

describe('diasDaSemana', () => {
  it('traz os 7 dias, de segunda a domingo', () => {
    expect(diasDaSemana('2026-09-21')).toEqual([
      '2026-09-21',
      '2026-09-22',
      '2026-09-23',
      '2026-09-24',
      '2026-09-25',
      '2026-09-26',
      '2026-09-27',
    ])
  })

  it('atravessa a virada do mês', () => {
    const dias = diasDaSemana('2026-09-28')

    expect(dias[2]).toBe('2026-09-30')
    expect(dias[3]).toBe('2026-10-01')
    expect(dias[6]).toBe('2026-10-04')
  })
})

describe('mesesDaSemana', () => {
  it('uma semana dentro do mês toca um mês só', () => {
    expect(mesesDaSemana(diasDaSemana('2026-09-21'))).toEqual(['2026-09'])
  })

  it('uma semana que cruza o mês toca os dois', () => {
    expect(mesesDaSemana(diasDaSemana('2026-09-28'))).toEqual([
      '2026-09',
      '2026-10',
    ])
  })

  it('cruza também a virada do ano', () => {
    expect(mesesDaSemana(diasDaSemana('2026-12-28'))).toEqual([
      '2026-12',
      '2027-01',
    ])
  })
})

describe('rotuloDaSemana', () => {
  it('mostra o intervalo com o nome do mês', () => {
    expect(rotuloDaSemana(diasDaSemana('2026-09-21'))).toBe('21 – 27 Setembro')
  })

  it('mostra os dois meses abreviados quando a semana cruza o mês', () => {
    expect(rotuloDaSemana(diasDaSemana('2026-09-28'))).toBe('28 Set – 4 Out')
  })
})

describe('numeroDaSemana', () => {
  it('divide o mês em blocos de 7 dias pela segunda-feira', () => {
    expect(numeroDaSemana('2026-09-07')).toBe(1)
    expect(numeroDaSemana('2026-09-14')).toBe(2)
    expect(numeroDaSemana('2026-09-21')).toBe(3)
  })

  it('a segunda do dia 28 em diante ainda é a semana 4', () => {
    expect(numeroDaSemana('2026-09-28')).toBe(4)
    expect(numeroDaSemana('2026-12-28')).toBe(4)
  })
})
