import { describe, expect, it } from 'vitest'
import type { Entrega, Evento } from '../../api/tipos'
import {
  contagemPrazo,
  estadoEvento,
  formatarHora,
  formatarHorarioEvento,
  legendaPrazo,
  ordenarEntregas,
  ordenarEventos,
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

function evento(parcial: Partial<Evento> = {}): Evento {
  return {
    id: 1,
    titulo: 'Consulta',
    data: '2026-10-21',
    inicio: '14:00:00',
    ...parcial,
  }
}

describe('estadoEvento', () => {
  // `agora` é 21/10/2026, 10:00.
  it('antes do início é normal', () => {
    expect(estadoEvento(evento(), agora)).toBe('normal')
  })

  it('dias à frente são sempre normais', () => {
    expect(estadoEvento(evento({ data: '2026-10-22', inicio: '08:00:00' }), agora)).toBe(
      'normal',
    )
  })

  it('entre o início e o término está acontecendo', () => {
    const rolando = evento({ inicio: '09:00:00', termino: '11:00:00' })
    expect(estadoEvento(rolando, agora)).toBe('agora')
  })

  it('no minuto exato do início já começou, e no do término já acabou', () => {
    const comecando = evento({ inicio: '10:00:00', termino: '11:00:00' })
    expect(estadoEvento(comecando, agora)).toBe('agora')
    const acabando = evento({ inicio: '09:00:00', termino: '10:00:00' })
    expect(estadoEvento(acabando, agora)).toBe('encerrado')
  })

  it('passado o término está encerrado', () => {
    const passou = evento({ inicio: '08:00:00', termino: '09:30:00' })
    expect(estadoEvento(passou, agora)).toBe('encerrado')
  })

  it('sem término, encerra quando começa', () => {
    expect(estadoEvento(evento({ inicio: '09:00:00' }), agora)).toBe('encerrado')
    expect(estadoEvento(evento({ inicio: '10:00:00' }), agora)).toBe('encerrado')
    expect(estadoEvento(evento({ inicio: '10:00:01' }), agora)).toBe('normal')
  })

  it('um dia que já passou está encerrado', () => {
    expect(estadoEvento(evento({ data: '2026-10-20' }), agora)).toBe('encerrado')
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

describe('legendaPrazo', () => {
  it('mostra o dia da semana e a data quando não há horário', () => {
    // 04/10/2026 é domingo.
    expect(legendaPrazo(entrega({ prazoData: '2026-10-04' }))).toBe('dom, 04 out')
  })

  it('acrescenta até que horas, quando há', () => {
    expect(
      legendaPrazo(entrega({ prazoData: '2026-10-04', prazoHorario: '14:30:00' })),
    ).toBe('dom, 04 out · até 14h30')
    expect(
      legendaPrazo(entrega({ prazoData: '2026-10-04', prazoHorario: '09:00:00' })),
    ).toBe('dom, 04 out · até 9h')
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

describe('contagemPrazo', () => {
  it('diz quantos dias faltam, sem urgência a partir de 2 dias', () => {
    expect(contagemPrazo(entrega({ prazoData: '2026-10-24' }), agora)).toEqual({
      texto: '3 dias',
      estado: 'normal',
      urgente: false,
    })
  })

  it('hoje é urgente', () => {
    expect(contagemPrazo(entrega({ prazoData: '2026-10-21' }), agora)).toEqual({
      texto: 'hoje',
      estado: 'urgente',
      urgente: true,
    })
  })

  it('menos de 24h para o prazo é urgente, mesmo que seja amanhã', () => {
    // Amanhã às 8h: faltam 22h.
    const cedo = entrega({ prazoData: '2026-10-22', prazoHorario: '08:00:00' })
    expect(contagemPrazo(cedo, agora)).toEqual({
      texto: 'amanhã',
      estado: 'urgente',
      urgente: true,
    })
  })

  it('de 24h a 48h é atenção, em amarelo', () => {
    // Amanhã sem horário vale até 23:59: faltam quase 38h.
    expect(contagemPrazo(entrega({ prazoData: '2026-10-22' }), agora)).toEqual({
      texto: 'amanhã',
      estado: 'atencao',
      urgente: false,
    })
    // Depois de amanhã às 9h: faltam 47h.
    const doisDias = entrega({ prazoData: '2026-10-23', prazoHorario: '09:00:00' })
    expect(contagemPrazo(doisDias, agora)).toEqual({
      texto: '2 dias',
      estado: 'atencao',
      urgente: false,
    })
  })

  it('48h ou mais é normal', () => {
    // Depois de amanhã às 10h: faltam exatamente 48h.
    const limite = entrega({ prazoData: '2026-10-23', prazoHorario: '10:00:00' })
    expect(contagemPrazo(limite, agora).estado).toBe('normal')
  })

  it('hoje com o horário já passado está atrasada desde hoje', () => {
    const passada = entrega({ prazoData: '2026-10-21', prazoHorario: '09:00:00' })
    expect(contagemPrazo(passada, agora)).toEqual({
      texto: 'atrasada',
      apoio: 'desde hoje',
      estado: 'atrasada',
      urgente: true,
    })
  })

  it('diz desde quando está atrasada', () => {
    expect(contagemPrazo(entrega({ prazoData: '2026-10-20' }), agora)).toEqual({
      texto: 'atrasada',
      apoio: 'desde ontem',
      estado: 'atrasada',
      urgente: true,
    })
    expect(contagemPrazo(entrega({ prazoData: '2026-10-17' }), agora)).toEqual({
      texto: 'atrasada',
      apoio: 'há 4 dias',
      estado: 'atrasada',
      urgente: true,
    })
  })

  it('entrega concluída não é urgente, mesmo com prazo vencido', () => {
    expect(
      contagemPrazo(entrega({ prazoData: '2026-10-10', concluida: true }), agora),
    ).toEqual({ texto: 'concluída', estado: 'concluida', urgente: false })
  })
})

describe('ordenarEventos', () => {
  // `agora` é 21/10/2026, 10:00.
  const passado = evento({ id: 1, titulo: 'Passado', inicio: '08:00:00', termino: '09:00:00' })
  const futuro = evento({ id: 2, titulo: 'Futuro', inicio: '14:00:00' })
  const acontecendo = evento({ id: 3, titulo: 'Agora', inicio: '09:00:00', termino: '11:00:00' })
  const outroDia = evento({ id: 4, titulo: 'Amanhã', data: '2026-10-22', inicio: '08:00:00' })

  it('põe os encerrados no fim, sem mudar a ordem dos demais', () => {
    expect(
      ordenarEventos([passado, futuro, acontecendo, outroDia], agora).map((e) => e.titulo),
    ).toEqual(['Futuro', 'Agora', 'Amanhã', 'Passado'])
  })

  it('não altera a lista recebida', () => {
    const original = [passado, futuro]
    ordenarEventos(original, agora)

    expect(original.map((e) => e.titulo)).toEqual(['Passado', 'Futuro'])
  })

  it('devolve vazio para lista vazia', () => {
    expect(ordenarEventos([], agora)).toEqual([])
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
