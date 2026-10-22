import { describe, expect, it } from 'vitest'
import type { Ritmo } from '../../api/tipos'
import {
  barrasSemanais,
  intervaloDoRitmo,
  melhorEPiorDia,
  percentual,
  ritmoPorDiaDaSemana,
  rotuloDoIntervalo,
  ultimoDiaDoMes,
} from './ritmo'

const HOJE = '2026-10-21'

describe('ultimoDiaDoMes', () => {
  it('devolve o último dia do mês', () => {
    expect(ultimoDiaDoMes('2026-09')).toBe('2026-09-30')
    expect(ultimoDiaDoMes('2026-10')).toBe('2026-10-31')
    expect(ultimoDiaDoMes('2028-02')).toBe('2028-02-29')
  })
})

describe('intervaloDoRitmo', () => {
  it('mês passado vai do dia 1 ao último dia', () => {
    expect(intervaloDoRitmo('2026-09', HOJE)).toEqual({
      de: '2026-09-01',
      ate: '2026-09-30',
    })
  })

  it('mês em andamento vai só até hoje', () => {
    expect(intervaloDoRitmo('2026-10', HOJE)).toEqual({
      de: '2026-10-01',
      ate: HOJE,
    })
  })

  it('mês que ainda não começou não tem intervalo', () => {
    expect(intervaloDoRitmo('2026-11', HOJE)).toBeNull()
    expect(intervaloDoRitmo('2027-01', HOJE)).toBeNull()
  })

  it('o primeiro dia do mês em andamento tem intervalo de um dia', () => {
    expect(intervaloDoRitmo('2026-10', '2026-10-01')).toEqual({
      de: '2026-10-01',
      ate: '2026-10-01',
    })
  })
})

describe('barrasSemanais', () => {
  const ritmo: Ritmo = {
    de: '2026-10-01',
    ate: HOJE,
    ritmoMedio: 72.4,
    semanas: [
      { rotulo: 'S1', de: '2026-10-01', ate: '2026-10-07', ritmo: 80 },
      { rotulo: 'S2', de: '2026-10-08', ate: '2026-10-14', ritmo: 65.7 },
      { rotulo: 'S3', de: '2026-10-15', ate: '2026-10-21', ritmo: 71.4 },
    ],
    dias: [],
  }

  it('sempre devolve quatro barras, de S1 a S4', () => {
    const barras = barrasSemanais('2026-10', ritmo, HOJE)

    expect(barras.map((barra) => barra.rotulo)).toEqual(['S1', 'S2', 'S3', 'S4'])
  })

  it('usa os dias de cada bloco, e o S4 vai até o fim do mês', () => {
    const barras = barrasSemanais('2026-10', ritmo, HOJE)

    expect(barras[0]).toMatchObject({ de: '2026-10-01', ate: '2026-10-07' })
    expect(barras[2]).toMatchObject({ de: '2026-10-15', ate: '2026-10-21' })
    expect(barras[3]).toMatchObject({ de: '2026-10-22', ate: '2026-10-31' })
  })

  it('traz o ritmo do servidor, e a semana que falta fica sem valor', () => {
    const barras = barrasSemanais('2026-10', ritmo, HOJE)

    expect(barras[1].ritmo).toBe(65.7)
    expect(barras[3].ritmo).toBeUndefined()
  })

  it('marca como atual só a semana de hoje', () => {
    const barras = barrasSemanais('2026-10', ritmo, HOJE)

    expect(barras.map((barra) => barra.atual)).toEqual([false, false, true, false])
  })

  it('num mês que não é o atual, nenhuma barra é atual', () => {
    const barras = barrasSemanais('2026-09', undefined, HOJE)

    expect(barras.some((barra) => barra.atual)).toBe(false)
  })

  it('sem ritmo (carregando ou mês futuro), as quatro barras vêm vazias', () => {
    const barras = barrasSemanais('2026-11', undefined, HOJE)

    expect(barras.every((barra) => barra.ritmo === undefined)).toBe(true)
  })

  it('fevereiro termina no dia 28 (ou 29)', () => {
    expect(barrasSemanais('2026-02', undefined, HOJE)[3].ate).toBe('2026-02-28')
    expect(barrasSemanais('2028-02', undefined, HOJE)[3].ate).toBe('2028-02-29')
  })
})

describe('rotuloDoIntervalo', () => {
  it('mostra os dois dias com zero à esquerda', () => {
    expect(rotuloDoIntervalo('2026-10-01', '2026-10-07')).toBe('01–07')
    expect(rotuloDoIntervalo('2026-10-22', '2026-10-31')).toBe('22–31')
  })
})

describe('ritmoPorDiaDaSemana', () => {
  it('devolve sempre sete dias, de segunda a domingo', () => {
    const medias = ritmoPorDiaDaSemana([])

    expect(medias.map((dia) => dia.inicial)).toEqual(['S', 'T', 'Q', 'Q', 'S', 'S', 'D'])
    expect(medias.every((dia) => dia.media === undefined)).toBe(true)
  })

  it('tira a média por dia da semana', () => {
    // 05/10 e 12/10 são segundas; 06/10 é terça.
    const medias = ritmoPorDiaDaSemana([
      { data: '2026-10-05', ritmo: 100 },
      { data: '2026-10-12', ritmo: 50 },
      { data: '2026-10-06', ritmo: 80 },
    ])

    expect(medias[0].media).toBe(75)
    expect(medias[1].media).toBe(80)
    expect(medias[2].media).toBeUndefined()
  })

  it('ignora o dia sem ritmo (nenhum hábito existia)', () => {
    const medias = ritmoPorDiaDaSemana([
      { data: '2026-10-05', ritmo: 60 },
      { data: '2026-10-12' },
    ])

    expect(medias[0].media).toBe(60)
  })
})

describe('melhorEPiorDia', () => {
  const medias = (valores: (number | undefined)[]) =>
    ritmoPorDiaDaSemana([]).map((dia, indice) => ({
      ...dia,
      media: valores[indice],
    }))

  it('acha o dia mais forte e o mais fraco', () => {
    const resultado = melhorEPiorDia(medias([80, 92, 70, 85, 60, 75, 90]))

    expect(resultado?.melhor.nome).toBe('Terça')
    expect(resultado?.pior.nome).toBe('Sexta')
  })

  it('ignora os dias sem valor', () => {
    const resultado = melhorEPiorDia(medias([undefined, 90, undefined, 40]))

    expect(resultado?.melhor.nome).toBe('Terça')
    expect(resultado?.pior.nome).toBe('Quinta')
  })

  it('sem dois dias com valor, não há o que comparar', () => {
    expect(melhorEPiorDia(medias([70]))).toBeNull()
    expect(melhorEPiorDia(medias([]))).toBeNull()
  })

  it('com todos iguais, não há melhor nem pior', () => {
    expect(melhorEPiorDia(medias([70, 70, 70]))).toBeNull()
  })

  it('no empate vale o primeiro, de segunda a domingo', () => {
    const resultado = melhorEPiorDia(medias([90, 50, 90, 50]))

    expect(resultado?.melhor.nome).toBe('Segunda')
    expect(resultado?.pior.nome).toBe('Terça')
  })
})

describe('percentual', () => {
  it('arredonda para o inteiro', () => {
    expect(percentual(82)).toBe('82%')
    expect(percentual(72.4)).toBe('72%')
    expect(percentual(72.5)).toBe('73%')
    expect(percentual(100)).toBe('100%')
  })

  it('sem valor mostra um traço', () => {
    expect(percentual(undefined)).toBe('–')
  })
})
