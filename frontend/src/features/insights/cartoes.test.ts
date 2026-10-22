import { describe, expect, it } from 'vitest'
import type {
  EstadoCelula,
  HabitoInsights,
  SemanaDoHabito,
} from '../../api/tipos'
import {
  contagemDoHabito,
  deltaEmPontos,
  montarCartoes,
  textoDaContagem,
  textoDoDelta,
} from './cartoes'

function diario(
  id: number,
  nome: string,
  estados: EstadoCelula[],
  parcial: Partial<HabitoInsights> = {},
): HabitoInsights {
  return {
    id,
    nome,
    tipo: 'DIARIO',
    excluido: false,
    dias: estados.map((estado, indice) => ({
      data: `2026-10-${String(indice + 1).padStart(2, '0')}`,
      estado,
    })),
    ...parcial,
  }
}

function semana(
  batida: boolean,
  contaNaConsistencia = true,
): SemanaDoHabito {
  return {
    de: '2026-10-05',
    ate: '2026-10-11',
    meta: 3,
    feitos: batida ? 3 : 1,
    batida,
    encerrada: true,
    contaNaConsistencia,
  }
}

function semanal(
  id: number,
  nome: string,
  semanas: SemanaDoHabito[],
  parcial: Partial<HabitoInsights> = {},
): HabitoInsights {
  return { id, nome, tipo: 'SEMANAL', excluido: false, dias: [], semanas, ...parcial }
}

describe('contagemDoHabito', () => {
  it('diário conta os dias concluídos sobre os dias considerados', () => {
    const habito = diario(1, 'Ler', [
      'CONCLUIDO',
      'CONCLUIDO',
      'NAO_REALIZADO',
      'CONCLUIDO',
    ])

    expect(contagemDoHabito(habito)).toEqual({ feitos: 3, total: 4 })
  })

  it('o dia de hoje, em aberto, entra na base', () => {
    const habito = diario(1, 'Ler', ['CONCLUIDO', 'EM_ABERTO'])

    expect(contagemDoHabito(habito)).toEqual({ feitos: 1, total: 2 })
  })

  it('dias futuros não entram', () => {
    const habito = diario(1, 'Ler', ['CONCLUIDO', 'DESCANSO_FUTURO', 'DESCANSO_FUTURO'])

    expect(contagemDoHabito(habito)).toEqual({ feitos: 1, total: 1 })
  })

  it('diário sem dia a considerar é null', () => {
    expect(contagemDoHabito(diario(1, 'Ler', []))).toBeNull()
    expect(contagemDoHabito(diario(1, 'Ler', ['DESCANSO_FUTURO']))).toBeNull()
  })

  it('semanal soma os dias feitos (até a meta) sobre a soma das metas', () => {
    const habito = semanal(2, 'Academia', [
      semana(true), // 3 / 3
      semana(false), // 1 / 3
      semana(true), // 3 / 3
      semana(false, false), // a semana em andamento não entra
    ])

    expect(contagemDoHabito(habito)).toEqual({ feitos: 7, total: 9 })
  })

  it('dia além da meta não compensa outra semana', () => {
    const forte = { ...semana(true), feitos: 5 } // meta 3, cinco feitos: vale 3

    const habito = semanal(2, 'Academia', [forte, semana(false)])

    expect(contagemDoHabito(habito)).toEqual({ feitos: 4, total: 6 })
  })

  it('semanal sem semana a considerar é null', () => {
    expect(contagemDoHabito(semanal(2, 'Academia', []))).toBeNull()
    expect(contagemDoHabito(semanal(2, 'Academia', [semana(false, false)]))).toBeNull()
  })
})

describe('textoDaContagem', () => {
  it('mostra feitos / total em dias', () => {
    expect(textoDaContagem({ feitos: 20, total: 20 })).toBe('20 / 20 dias')
    expect(textoDaContagem({ feitos: 7, total: 9 })).toBe('7 / 9 dias')
  })

  it('sem contagem, avisa que ainda não há dados', () => {
    expect(textoDaContagem(null)).toBe('Sem dados ainda')
  })
})

describe('deltaEmPontos', () => {
  it('é a diferença entre os números inteiros mostrados', () => {
    // A tela mostra 87% e 79%: a diferença é 8, não os 7,2 dos valores crus.
    expect(deltaEmPontos(86.6, 79.4)).toBe(8)
  })

  it('pode ser negativo ou zero', () => {
    expect(deltaEmPontos(70, 80)).toBe(-10)
    expect(deltaEmPontos(75, 75)).toBe(0)
  })

  it('sem um dos lados, não há comparação', () => {
    expect(deltaEmPontos(80, undefined)).toBeNull()
    expect(deltaEmPontos(undefined, 80)).toBeNull()
    expect(deltaEmPontos(undefined, undefined)).toBeNull()
  })
})

describe('textoDoDelta', () => {
  it('usa mais, menos de verdade e zero', () => {
    expect(textoDoDelta(7)).toBe('+7 pts')
    expect(textoDoDelta(-3)).toBe('−3 pts')
    expect(textoDoDelta(0)).toBe('0 pts')
  })
})

describe('montarCartoes', () => {
  it('ordena do mais para o menos consistente', () => {
    const cartoes = montarCartoes(
      [
        diario(1, 'Ler', [], { consistencia: 70 }),
        diario(2, 'Sono', [], { consistencia: 95 }),
        diario(3, 'Estudo', [], { consistencia: 86 }),
      ],
      undefined,
    )

    expect(cartoes.map((c) => c.nome)).toEqual(['Sono', 'Estudo', 'Ler'])
  })

  it('no empate vale o nome, e quem não tem valor vai por último', () => {
    const cartoes = montarCartoes(
      [
        diario(1, 'Zumba', [], { consistencia: 80 }),
        diario(2, 'Ler', []),
        diario(3, 'Água', [], { consistencia: 80 }),
      ],
      undefined,
    )

    expect(cartoes.map((c) => c.nome)).toEqual(['Água', 'Zumba', 'Ler'])
  })

  it('calcula o delta com o mesmo hábito do mês anterior', () => {
    const cartoes = montarCartoes(
      [diario(1, 'Ler', [], { consistencia: 90 })],
      [
        diario(1, 'Ler', [], { consistencia: 80 }),
        diario(9, 'Outro', [], { consistencia: 10 }),
      ],
    )

    expect(cartoes[0].delta).toBe(10)
  })

  it('hábito que não existia no mês anterior fica sem delta', () => {
    const cartoes = montarCartoes(
      [diario(5, 'Novo', [], { consistencia: 90 })],
      [diario(1, 'Ler', [], { consistencia: 80 })],
    )

    expect(cartoes[0].delta).toBeNull()
  })

  it('sem o mês anterior (ainda carregando), nenhum cartão tem delta', () => {
    const cartoes = montarCartoes([diario(1, 'Ler', [], { consistencia: 90 })], undefined)

    expect(cartoes[0].delta).toBeNull()
  })

  it('leva a contagem e o estado de excluído', () => {
    const cartoes = montarCartoes(
      [diario(1, 'Ler', ['CONCLUIDO'], { consistencia: 100, excluido: true })],
      undefined,
    )

    expect(cartoes[0].contagem).toEqual({ feitos: 1, total: 1 })
    expect(cartoes[0].excluido).toBe(true)
  })

  it('não altera a lista original', () => {
    const original = [
      diario(1, 'Ler', [], { consistencia: 50 }),
      diario(2, 'Sono', [], { consistencia: 90 }),
    ]

    montarCartoes(original, undefined)

    expect(original.map((h) => h.nome)).toEqual(['Ler', 'Sono'])
  })
})
