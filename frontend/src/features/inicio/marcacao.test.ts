import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import type { Inicio } from '../../api/tipos'
import { chavesInicio } from './chaves'
import { aplicarMarcacao, aplicarMarcacaoNoCache } from './marcacao'

function inicio(): Inicio {
  return {
    data: '2026-10-21',
    upcoming: { entregas: [], eventos: [] },
    habitos: [
      { id: 1, nome: 'Ler', tipo: 'DIARIO', feito: false },
      { id: 2, nome: 'Sono', tipo: 'DIARIO', feito: true },
      { id: 4, nome: 'Academia', tipo: 'SEMANAL', feito: false, feitos: 1, meta: 3 },
      { id: 5, nome: 'Corrida', tipo: 'SEMANAL', feito: true, feitos: 2, meta: 3 },
    ],
  }
}

function habito(resultado: Inicio, id: number) {
  const encontrado = resultado.habitos.find((h) => h.id === id)
  if (encontrado === undefined) throw new Error(`hábito ${id} ausente`)
  return encontrado
}

describe('aplicarMarcacao', () => {
  it('marca um hábito diário', () => {
    const resultado = aplicarMarcacao(inicio(), 1, true)

    expect(habito(resultado, 1).feito).toBe(true)
    expect('feitos' in habito(resultado, 1)).toBe(false)
  })

  it('desmarca um hábito diário', () => {
    expect(habito(aplicarMarcacao(inicio(), 2, false), 2).feito).toBe(false)
  })

  it('sobe o contador ao marcar um semanal', () => {
    const academia = habito(aplicarMarcacao(inicio(), 4, true), 4)

    expect(academia.feito).toBe(true)
    expect(academia.feitos).toBe(2)
    expect(academia.meta).toBe(3)
  })

  it('desce o contador ao desmarcar um semanal', () => {
    const corrida = habito(aplicarMarcacao(inicio(), 5, false), 5)

    expect(corrida.feito).toBe(false)
    expect(corrida.feitos).toBe(1)
  })

  it('não deixa o contador ficar abaixo de zero', () => {
    const base: Inicio = {
      data: '2026-10-21',
      upcoming: { entregas: [], eventos: [] },
      habitos: [
        { id: 7, nome: 'Yoga', tipo: 'SEMANAL', feito: true, feitos: 0, meta: 3 },
      ],
    }

    expect(habito(aplicarMarcacao(base, 7, false), 7).feitos).toBe(0)
  })

  it('permite ultrapassar a meta', () => {
    const base: Inicio = {
      data: '2026-10-21',
      upcoming: { entregas: [], eventos: [] },
      habitos: [
        { id: 8, nome: 'Treino', tipo: 'SEMANAL', feito: false, feitos: 3, meta: 3 },
      ],
    }

    expect(habito(aplicarMarcacao(base, 8, true), 8).feitos).toBe(4)
  })

  it('não mexe em nada quando o estado já é o pedido', () => {
    const original = inicio()

    expect(aplicarMarcacao(original, 1, false)).toBe(original)
    expect(aplicarMarcacao(original, 5, true)).toBe(original)
  })

  it('devolve o mesmo objeto para um id desconhecido', () => {
    const original = inicio()

    expect(aplicarMarcacao(original, 99, true)).toBe(original)
  })

  it('não altera os outros hábitos', () => {
    const original = inicio()
    const resultado = aplicarMarcacao(original, 4, true)

    expect(habito(resultado, 1)).toBe(habito(original, 1))
    expect(habito(resultado, 5)).toBe(habito(original, 5))
  })

  it('não altera o objeto original', () => {
    const original = inicio()
    aplicarMarcacao(original, 4, true)

    expect(original).toEqual(inicio())
  })
})

describe('aplicarMarcacaoNoCache', () => {
  const data = '2026-10-21'

  function clienteComInicio(): QueryClient {
    const cliente = new QueryClient()
    cliente.setQueryData(chavesInicio.dia(data), inicio())
    return cliente
  }

  function lido(cliente: QueryClient): Inicio {
    const atual = cliente.getQueryData<Inicio>(chavesInicio.dia(data))
    if (atual === undefined) throw new Error('cache vazio')
    return atual
  }

  it('marca no cache do dia', () => {
    const cliente = clienteComInicio()

    aplicarMarcacaoNoCache(cliente, data, 4, true)

    expect(habito(lido(cliente), 4).feitos).toBe(2)
  })

  it('reverter um hábito não desfaz a marcação de outro em voo', () => {
    const cliente = clienteComInicio()

    aplicarMarcacaoNoCache(cliente, data, 1, true)
    aplicarMarcacaoNoCache(cliente, data, 4, true)
    // O de id 4 falhou: reverte só ele.
    aplicarMarcacaoNoCache(cliente, data, 4, false)

    expect(habito(lido(cliente), 4).feito).toBe(false)
    expect(habito(lido(cliente), 4).feitos).toBe(1)
    expect(habito(lido(cliente), 1).feito).toBe(true)
  })

  it('não faz nada quando o dia ainda não está em cache', () => {
    const cliente = new QueryClient()

    aplicarMarcacaoNoCache(cliente, data, 1, true)

    expect(cliente.getQueryData(chavesInicio.dia(data))).toBeUndefined()
  })

  it('não mexe no cache de outro dia', () => {
    const cliente = clienteComInicio()
    cliente.setQueryData(chavesInicio.dia('2026-10-20'), inicio())

    aplicarMarcacaoNoCache(cliente, data, 1, true)

    const outroDia = cliente.getQueryData<Inicio>(chavesInicio.dia('2026-10-20'))
    expect(outroDia && habito(outroDia, 1).feito).toBe(false)
  })
})
