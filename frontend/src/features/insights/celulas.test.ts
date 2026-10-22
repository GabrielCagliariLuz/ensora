import { describe, expect, it } from 'vitest'
import type {
  Consistencia,
  EstadoCelula,
  HabitoInsights,
} from '../../api/tipos'
import { estadosPorDia, mesclarHabitos, ordenarPorNome } from './celulas'

function diario(
  id: number,
  dias: [string, EstadoCelula][],
  parcial: Partial<HabitoInsights> = {},
): HabitoInsights {
  return {
    id,
    nome: `H${id}`,
    tipo: 'DIARIO',
    excluido: false,
    dias: dias.map(([data, estado]) => ({ data, estado })),
    ...parcial,
  }
}

describe('estadosPorDia', () => {
  it('indexa o estado de cada dia com célula', () => {
    const mapa = estadosPorDia(
      diario(1, [
        ['2026-09-21', 'CONCLUIDO'],
        ['2026-09-22', 'NAO_REALIZADO'],
      ]),
    )

    expect(mapa.get('2026-09-21')).toBe('CONCLUIDO')
    expect(mapa.get('2026-09-22')).toBe('NAO_REALIZADO')
    expect(mapa.get('2026-09-23')).toBeUndefined()
  })
})

describe('ordenarPorNome', () => {
  const habito = (id: number, nome: string) => diario(id, [], { nome })

  it('ordena em ordem alfabética e junta nomes que começam igual', () => {
    const resultado = ordenarPorNome([
      habito(1, 'Sono'),
      habito(2, 'Estudar Programação'),
      habito(3, 'Ler'),
      habito(4, 'Estudar Inglês'),
    ])

    expect(resultado.map((h) => h.nome)).toEqual([
      'Estudar Inglês',
      'Estudar Programação',
      'Ler',
      'Sono',
    ])
  })

  it('não diferencia maiúsculas nem acentos', () => {
    const resultado = ordenarPorNome([
      habito(1, 'Zumba'),
      habito(2, 'água'),
      habito(3, 'Alimentação'),
    ])

    expect(resultado.map((h) => h.nome)).toEqual([
      'água',
      'Alimentação',
      'Zumba',
    ])
  })

  it('nomes iguais mantêm a ordem de criação', () => {
    const resultado = ordenarPorNome([habito(7, 'Ler'), habito(3, 'Ler')])

    expect(resultado.map((h) => h.id)).toEqual([3, 7])
  })

  it('não altera a lista original', () => {
    const original = [habito(1, 'Sono'), habito(2, 'Ler')]

    ordenarPorNome(original)

    expect(original.map((h) => h.nome)).toEqual(['Sono', 'Ler'])
  })
})

describe('mesclarHabitos', () => {
  function mes(mesRef: string, habitos: HabitoInsights[]): Consistencia {
    return { mes: mesRef, habitos }
  }

  it('junta os dias do mesmo hábito nos dois meses', () => {
    const resultado = mesclarHabitos([
      mes('2026-09', [diario(1, [['2026-09-30', 'CONCLUIDO']])]),
      mes('2026-10', [diario(1, [['2026-10-01', 'EM_ABERTO']])]),
    ])

    expect(resultado).toHaveLength(1)
    expect(resultado[0].dias.map((celula) => celula.data)).toEqual([
      '2026-09-30',
      '2026-10-01',
    ])
  })

  it('não repete o dia que vem nos dois meses', () => {
    const resultado = mesclarHabitos([
      mes('2026-09', [diario(1, [['2026-09-30', 'CONCLUIDO']])]),
      mes('2026-10', [diario(1, [['2026-09-30', 'CONCLUIDO']])]),
    ])

    expect(resultado[0].dias).toHaveLength(1)
  })

  it('mantém o hábito que só existe em um dos meses, em ordem de criação', () => {
    const resultado = mesclarHabitos([
      mes('2026-09', [diario(2, [['2026-09-30', 'CONCLUIDO']])]),
      mes('2026-10', [
        diario(1, [['2026-10-01', 'CONCLUIDO']]),
        diario(2, [['2026-10-01', 'EM_ABERTO']]),
      ]),
    ])

    expect(resultado.map((habito) => habito.id)).toEqual([1, 2])
  })

  it('junta as semanas do hábito semanal sem repetir', () => {
    const semana = (de: string, ate: string) => ({
      de,
      ate,
      meta: 3,
      feitos: 1,
      batida: false,
      encerrada: true,
      contaNaConsistencia: true,
    })
    const semanal = (semanas: ReturnType<typeof semana>[]) =>
      diario(5, [], { tipo: 'SEMANAL', semanas })

    const resultado = mesclarHabitos([
      mes('2026-09', [semanal([semana('2026-09-28', '2026-10-04')])]),
      mes('2026-10', [
        semanal([
          semana('2026-09-28', '2026-10-04'),
          semana('2026-10-05', '2026-10-11'),
        ]),
      ]),
    ])

    expect(resultado[0].semanas?.map((s) => s.de)).toEqual([
      '2026-09-28',
      '2026-10-05',
    ])
  })

  it('não altera as respostas originais', () => {
    const setembro = mes('2026-09', [diario(1, [['2026-09-30', 'CONCLUIDO']])])
    const outubro = mes('2026-10', [diario(1, [['2026-10-01', 'EM_ABERTO']])])

    mesclarHabitos([setembro, outubro])

    expect(setembro.habitos[0].dias).toHaveLength(1)
  })

  it('sem respostas, devolve lista vazia', () => {
    expect(mesclarHabitos([])).toEqual([])
  })
})
