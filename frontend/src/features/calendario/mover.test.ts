import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import type { Calendario } from '../../api/tipos'
import type { Registro } from './agenda'
import { chavesCalendario } from './chaves'
import {
  comNovaData,
  lerRegistroDoCache,
  substituirRegistro,
  substituirRegistroNoCache,
} from './mover'

const DE = '2026-09-27'
const ATE = '2026-10-31'

function calendario(): Calendario {
  return {
    de: DE,
    ate: ATE,
    entregas: [
      { id: 1, titulo: 'A', prazoData: '2026-10-05', concluida: false },
      { id: 2, titulo: 'B', prazoData: '2026-10-06', concluida: false },
    ],
    eventos: [
      { id: 1, titulo: 'Consulta', data: '2026-10-05', inicio: '09:00:00' },
      { id: 2, titulo: 'Aula', data: '2026-10-06', inicio: '19:00:00' },
    ],
  }
}

function clienteComCalendario(): QueryClient {
  const cliente = new QueryClient()
  cliente.setQueryData(chavesCalendario.intervalo(DE, ATE), calendario())
  return cliente
}

function lido(cliente: QueryClient): Calendario {
  const atual = cliente.getQueryData<Calendario>(
    chavesCalendario.intervalo(DE, ATE),
  )
  if (atual === undefined) throw new Error('cache vazio')
  return atual
}

function registroDe(tipo: Registro['tipo'], id: number): Registro {
  const registro = lerRegistroDoCache(clienteComCalendario(), tipo, id)
  if (registro === undefined) throw new Error('registro ausente')
  return registro
}

describe('comNovaData', () => {
  it('muda a data do evento', () => {
    const movido = comNovaData(registroDe('EVENTO', 1), '2026-10-09')

    expect(movido.tipo === 'EVENTO' && movido.evento.data).toBe('2026-10-09')
  })

  it('muda o prazo da entrega', () => {
    const movido = comNovaData(registroDe('ENTREGA', 1), '2026-10-09')

    expect(movido.tipo === 'ENTREGA' && movido.entrega.prazoData).toBe(
      '2026-10-09',
    )
  })

  it('não altera o original', () => {
    const original = registroDe('EVENTO', 1)

    comNovaData(original, '2026-10-09')

    expect(original.tipo === 'EVENTO' && original.evento.data).toBe(
      '2026-10-05',
    )
  })
})

describe('substituirRegistro', () => {
  it('troca só o evento de mesmo id', () => {
    const original = calendario()
    const movido = comNovaData(registroDe('EVENTO', 1), '2026-10-09')
    const resultado = substituirRegistro(original, movido)

    expect(resultado.eventos[0].data).toBe('2026-10-09')
    expect(resultado.eventos[1]).toBe(original.eventos[1])
    expect(resultado.entregas).toBe(original.entregas)
  })

  it('evento e entrega de mesmo id não se confundem', () => {
    const original = calendario()
    const movida = comNovaData(registroDe('ENTREGA', 1), '2026-10-09')
    const resultado = substituirRegistro(original, movida)

    expect(resultado.entregas[0].prazoData).toBe('2026-10-09')
    expect(resultado.eventos[0].data).toBe('2026-10-05')
  })

  it('devolve o mesmo objeto para um id desconhecido', () => {
    const original = calendario()
    const fantasma: Registro = {
      tipo: 'EVENTO',
      evento: { id: 99, titulo: 'X', data: '2026-10-09', inicio: '09:00:00' },
    }

    expect(substituirRegistro(original, fantasma)).toBe(original)
  })
})

describe('movimento no cache', () => {
  it('lê o registro como está em alguma grade', () => {
    const cliente = clienteComCalendario()

    expect(lerRegistroDoCache(cliente, 'EVENTO', 2)).toBeDefined()
    expect(lerRegistroDoCache(cliente, 'ENTREGA', 99)).toBeUndefined()
  })

  it('move o registro em todas as grades que o contêm', () => {
    const cliente = clienteComCalendario()
    cliente.setQueryData(
      chavesCalendario.intervalo('2026-10-25', '2026-12-05'),
      calendario(),
    )

    substituirRegistroNoCache(
      cliente,
      comNovaData(registroDe('EVENTO', 1), '2026-10-09'),
    )

    expect(lido(cliente).eventos[0].data).toBe('2026-10-09')
    expect(
      cliente.getQueryData<Calendario>(
        chavesCalendario.intervalo('2026-10-25', '2026-12-05'),
      )?.eventos[0].data,
    ).toBe('2026-10-09')
  })

  it('desfazer um movimento não reverte o de outro registro', () => {
    const cliente = clienteComCalendario()
    const antes = registroDe('EVENTO', 1)

    substituirRegistroNoCache(cliente, comNovaData(antes, '2026-10-09'))
    substituirRegistroNoCache(
      cliente,
      comNovaData(registroDe('EVENTO', 2), '2026-10-10'),
    )
    substituirRegistroNoCache(cliente, antes)

    expect(lido(cliente).eventos[0].data).toBe('2026-10-05')
    expect(lido(cliente).eventos[1].data).toBe('2026-10-10')
  })

  it('não faz nada sem grade em cache', () => {
    const cliente = new QueryClient()

    substituirRegistroNoCache(
      cliente,
      comNovaData(registroDe('EVENTO', 1), '2026-10-09'),
    )

    expect(cliente.getQueryCache().getAll()).toHaveLength(0)
  })
})
