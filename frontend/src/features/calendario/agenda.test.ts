import { describe, expect, it } from 'vitest'
import type { Entrega, Evento } from '../../api/tipos'
import { agruparPorDia, horaCurta, registrosDoDia } from './agenda'
import type { Registro } from './agenda'

function entrega(parcial: Partial<Entrega> = {}): Entrega {
  return {
    id: 1,
    titulo: 'Trabalho',
    prazoData: '2026-10-21',
    concluida: false,
    ...parcial,
  }
}

function evento(parcial: Partial<Evento> = {}): Evento {
  return {
    id: 1,
    titulo: 'Consulta',
    data: '2026-10-21',
    inicio: '09:00:00',
    ...parcial,
  }
}

function titulos(registros: Registro[]): string[] {
  return registros.map((r) => (r.tipo === 'EVENTO' ? r.evento.titulo : r.entrega.titulo))
}

describe('horaCurta', () => {
  it('corta os segundos', () => {
    expect(horaCurta('09:00:00')).toBe('09:00')
    expect(horaCurta('19:15:00')).toBe('19:15')
    expect(horaCurta('13:40')).toBe('13:40')
  })
})

describe('agruparPorDia', () => {
  it('põe entregas pelo prazo e eventos pela data', () => {
    const dias = agruparPorDia({
      entregas: [
        entrega({ id: 1, prazoData: '2026-10-21' }),
        entrega({ id: 2, prazoData: '2026-10-22' }),
      ],
      eventos: [evento({ id: 3, data: '2026-10-21' })],
    })

    expect(dias.get('2026-10-21')?.entregas.map((e) => e.id)).toEqual([1])
    expect(dias.get('2026-10-21')?.eventos.map((e) => e.id)).toEqual([3])
    expect(dias.get('2026-10-22')?.entregas.map((e) => e.id)).toEqual([2])
    expect(dias.get('2026-10-22')?.eventos).toEqual([])
  })

  it('não cria entrada para dia sem registro', () => {
    const dias = agruparPorDia({ entregas: [], eventos: [] })

    expect(dias.size).toBe(0)
    expect(dias.get('2026-10-21')).toBeUndefined()
  })

  it('mantém a ordem do servidor dentro do dia', () => {
    const dias = agruparPorDia({
      entregas: [entrega({ id: 5 }), entrega({ id: 3 }), entrega({ id: 9 })],
      eventos: [],
    })

    expect(dias.get('2026-10-21')?.entregas.map((e) => e.id)).toEqual([5, 3, 9])
  })

  it('inclui entregas concluídas', () => {
    const dias = agruparPorDia({
      entregas: [entrega({ concluida: true })],
      eventos: [],
    })

    expect(dias.get('2026-10-21')?.entregas[0].concluida).toBe(true)
  })
})

describe('registrosDoDia', () => {
  it('devolve vazio para um dia sem registros', () => {
    expect(registrosDoDia(undefined)).toEqual([])
    expect(registrosDoDia({ entregas: [], eventos: [] })).toEqual([])
  })

  it('ordena eventos e entregas por horário', () => {
    const lista = registrosDoDia({
      entregas: [entrega({ titulo: 'Entrega 14h', prazoHorario: '14:00:00' })],
      eventos: [
        evento({ titulo: 'Evento 19h', inicio: '19:00:00' }),
        evento({ titulo: 'Evento 9h', inicio: '09:00:00' }),
      ],
    })

    expect(titulos(lista)).toEqual(['Evento 9h', 'Entrega 14h', 'Evento 19h'])
  })

  it('põe a entrega sem horário no fim do dia', () => {
    const lista = registrosDoDia({
      entregas: [entrega({ titulo: 'Sem horário' })],
      eventos: [evento({ titulo: 'Noite', inicio: '23:30:00' })],
    })

    expect(titulos(lista)).toEqual(['Noite', 'Sem horário'])
  })

  it('no mesmo horário o evento vem antes da entrega', () => {
    const lista = registrosDoDia({
      entregas: [entrega({ titulo: 'Entrega', prazoHorario: '10:00:00' })],
      eventos: [evento({ titulo: 'Evento', inicio: '10:00:00' })],
    })

    expect(titulos(lista)).toEqual(['Evento', 'Entrega'])
  })

  it('mantém a ordem do servidor entre registros do mesmo tipo e horário', () => {
    const lista = registrosDoDia({
      entregas: [],
      eventos: [
        evento({ id: 2, titulo: 'B', inicio: '10:00:00' }),
        evento({ id: 1, titulo: 'A', inicio: '10:00:00' }),
      ],
    })

    expect(titulos(lista)).toEqual(['B', 'A'])
  })

  it('identifica o tipo de cada registro', () => {
    const lista = registrosDoDia({
      entregas: [entrega({ prazoHorario: '11:00:00' })],
      eventos: [evento({ inicio: '09:00:00' })],
    })

    expect(lista.map((r) => r.tipo)).toEqual(['EVENTO', 'ENTREGA'])
  })
})
