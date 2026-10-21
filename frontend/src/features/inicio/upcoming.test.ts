import { describe, expect, it } from 'vitest'
import type { Entrega } from '../../api/tipos'
import {
  formatarHora,
  formatarHorarioEvento,
  ordenarEntregas,
  rotuloPrazo,
  situacaoPrazo,
  textoDiasEvento,
} from './upcoming'

function entrega(parcial: Partial<Entrega> = {}): Entrega {
  return {
    id: 1,
    titulo: 'Trabalho',
    prazoData: '2026-10-24',
    concluida: false,
    ...parcial,
  }
}

// Quarta-feira, 21/10/2026, 10:00 no horário local.
const agora = new Date(2026, 9, 21, 10, 0, 0)

describe('formatarHora', () => {
  it('tira os segundos e o zero à esquerda', () => {
    expect(formatarHora('20:30:00')).toBe('20h30')
    expect(formatarHora('09:05:00')).toBe('9h05')
    expect(formatarHora('14:30')).toBe('14h30')
  })

  it('omite os minutos quando é hora cheia', () => {
    expect(formatarHora('19:00:00')).toBe('19h')
    expect(formatarHora('09:00:00')).toBe('9h')
    expect(formatarHora('00:00:00')).toBe('0h')
  })
})

describe('formatarHorarioEvento', () => {
  it('mostra só o início quando não há término', () => {
    expect(formatarHorarioEvento('20:30:00')).toBe('20h30')
  })

  it('mostra o intervalo quando há término', () => {
    expect(formatarHorarioEvento('19:00:00', '22:00:00')).toBe('19h a 22h')
    expect(formatarHorarioEvento('09:00:00', '10:15:00')).toBe('9h a 10h15')
  })
})

describe('textoDiasEvento', () => {
  it('diz hoje, amanhã e N dias', () => {
    expect(textoDiasEvento('2026-10-21', '2026-10-21')).toBe('hoje')
    expect(textoDiasEvento('2026-10-22', '2026-10-21')).toBe('amanhã')
    expect(textoDiasEvento('2026-10-24', '2026-10-21')).toBe('3 dias')
    expect(textoDiasEvento('2026-10-25', '2026-10-21')).toBe('4 dias')
  })

  it('trata uma data passada como hoje', () => {
    expect(textoDiasEvento('2026-10-20', '2026-10-21')).toBe('hoje')
  })
})

describe('rotuloPrazo', () => {
  it('mostra só a data quando não há horário', () => {
    expect(rotuloPrazo(entrega({ prazoData: '2026-10-04' }))).toBe('04 out')
  })

  it('acrescenta o horário quando há', () => {
    expect(
      rotuloPrazo(entrega({ prazoData: '2026-10-04', prazoHorario: '14:30:00' })),
    ).toBe('04 out, 14h30')
  })
})

describe('situacaoPrazo', () => {
  it('diz em quantos dias vence, sem urgência a partir de 2 dias', () => {
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-24' }), agora)).toEqual({
      texto: 'vence em 3 dias',
      urgente: false,
    })
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-23' }), agora)).toEqual({
      texto: 'vence em 2 dias',
      urgente: false,
    })
  })

  it('vence amanhã é urgente', () => {
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-22' }), agora)).toEqual({
      texto: 'vence amanhã',
      urgente: true,
    })
  })

  it('vence hoje é urgente, com ou sem horário', () => {
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-21' }), agora)).toEqual({
      texto: 'vence hoje',
      urgente: true,
    })
    expect(
      situacaoPrazo(
        entrega({ prazoData: '2026-10-21', prazoHorario: '23:00:00' }),
        agora,
      ),
    ).toEqual({ texto: 'vence hoje', urgente: true })
  })

  it('hoje com o horário já passado está atrasada', () => {
    expect(
      situacaoPrazo(
        entrega({ prazoData: '2026-10-21', prazoHorario: '09:59:00' }),
        agora,
      ),
    ).toEqual({ texto: 'venceu hoje', urgente: true })
  })

  it('no minuto exato do horário ainda não está atrasada', () => {
    expect(
      situacaoPrazo(
        entrega({ prazoData: '2026-10-21', prazoHorario: '10:00:00' }),
        agora,
      ).texto,
    ).toBe('vence hoje')
  })

  it('sem horário, só atrasa depois das 23:59', () => {
    const noite = new Date(2026, 9, 21, 23, 58, 0)
    const virou = new Date(2026, 9, 21, 23, 59, 30)
    const hoje = entrega({ prazoData: '2026-10-21' })

    expect(situacaoPrazo(hoje, noite).texto).toBe('vence hoje')
    expect(situacaoPrazo(hoje, virou).texto).toBe('venceu hoje')
  })

  it('prazo de ontem está atrasado e é urgente', () => {
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-20' }), agora)).toEqual({
      texto: 'venceu ontem',
      urgente: true,
    })
  })

  it('prazo mais antigo diz há quantos dias venceu', () => {
    expect(situacaoPrazo(entrega({ prazoData: '2026-10-16' }), agora)).toEqual({
      texto: 'venceu há 5 dias',
      urgente: true,
    })
  })

  it('entrega concluída não é urgente, mesmo com prazo vencido', () => {
    expect(
      situacaoPrazo(entrega({ prazoData: '2026-10-20', concluida: true }), agora),
    ).toEqual({ texto: 'concluída', urgente: false })
  })
})

describe('ordenarEntregas', () => {
  const a = entrega({ id: 1, titulo: 'A' })
  const b = entrega({ id: 2, titulo: 'B', concluida: true })
  const c = entrega({ id: 3, titulo: 'C' })
  const d = entrega({ id: 4, titulo: 'D', concluida: true })

  it('põe as pendentes primeiro e as concluídas depois', () => {
    expect(ordenarEntregas([a, b, c, d]).map((e) => e.titulo)).toEqual([
      'A',
      'C',
      'B',
      'D',
    ])
  })

  it('mantém a ordem original dentro de cada grupo', () => {
    expect(ordenarEntregas([d, c, b, a]).map((e) => e.titulo)).toEqual([
      'C',
      'A',
      'D',
      'B',
    ])
  })

  it('não altera a lista recebida', () => {
    const original = [a, b, c]
    ordenarEntregas(original)

    expect(original.map((e) => e.titulo)).toEqual(['A', 'B', 'C'])
  })

  it('devolve vazio para lista vazia', () => {
    expect(ordenarEntregas([])).toEqual([])
  })
})
