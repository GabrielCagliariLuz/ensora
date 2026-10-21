import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import type { Entrega, Inicio } from '../../api/tipos'
import { chavesInicio } from './chaves'
import {
  aplicarConclusao,
  aplicarConclusaoNoCache,
  lerEntregaDoCache,
  restaurarEntregaNoCache,
  substituirEntrega,
} from './conclusao'

const HOJE = '2026-10-21'

function entregas(): Entrega[] {
  return [
    { id: 1, titulo: 'A', prazoData: '2026-10-22', concluida: false },
    {
      id: 2,
      titulo: 'B',
      prazoData: '2026-10-20',
      concluida: true,
      concluidaEm: '2026-10-20T15:00:00Z',
      concluidaData: '2026-10-20',
    },
    { id: 3, titulo: 'C', prazoData: '2026-10-23', concluida: false },
  ]
}

function inicio(): Inicio {
  return {
    data: HOJE,
    habitos: [],
    upcoming: { entregas: entregas(), eventos: [] },
  }
}

function entrega(resultado: Inicio, id: number): Entrega {
  const encontrada = resultado.upcoming.entregas.find((e) => e.id === id)
  if (encontrada === undefined) throw new Error(`entrega ${id} ausente`)
  return encontrada
}

describe('aplicarConclusao', () => {
  it('conclui a entrega com a data de hoje', () => {
    const concluida = entrega(aplicarConclusao(inicio(), 1, true, HOJE), 1)

    expect(concluida.concluida).toBe(true)
    expect(concluida.concluidaData).toBe(HOJE)
  })

  it('desfaz a conclusão e limpa a data e o instante', () => {
    const desfeita = entrega(aplicarConclusao(inicio(), 2, false, HOJE), 2)

    expect(desfeita.concluida).toBe(false)
    expect('concluidaData' in desfeita).toBe(false)
    expect('concluidaEm' in desfeita).toBe(false)
  })

  it('não mexe em nada quando o estado já é o pedido', () => {
    const original = inicio()

    expect(aplicarConclusao(original, 1, false, HOJE)).toBe(original)
    expect(aplicarConclusao(original, 2, true, HOJE)).toBe(original)
  })

  it('devolve o mesmo objeto para um id desconhecido', () => {
    const original = inicio()

    expect(aplicarConclusao(original, 99, true, HOJE)).toBe(original)
  })

  it('não altera as outras entregas nem o original', () => {
    const original = inicio()
    const resultado = aplicarConclusao(original, 1, true, HOJE)

    expect(entrega(resultado, 3)).toBe(entrega(original, 3))
    expect(original).toEqual(inicio())
  })
})

describe('substituirEntrega', () => {
  it('troca só a entrega de mesmo id', () => {
    const nova: Entrega = { ...entregas()[2], titulo: 'C editada' }
    const resultado = substituirEntrega(inicio(), nova)

    expect(entrega(resultado, 3).titulo).toBe('C editada')
    expect(entrega(resultado, 1).titulo).toBe('A')
  })

  it('devolve o mesmo objeto para um id desconhecido', () => {
    const original = inicio()

    expect(substituirEntrega(original, { ...entregas()[0], id: 99 })).toBe(original)
  })
})

describe('conclusão no cache', () => {
  function clienteComInicio(): QueryClient {
    const cliente = new QueryClient()
    cliente.setQueryData(chavesInicio.dia(HOJE), inicio())
    return cliente
  }

  function lido(cliente: QueryClient): Inicio {
    const atual = cliente.getQueryData<Inicio>(chavesInicio.dia(HOJE))
    if (atual === undefined) throw new Error('cache vazio')
    return atual
  }

  it('lê a entrega como está no cache', () => {
    const cliente = clienteComInicio()

    expect(lerEntregaDoCache(cliente, HOJE, 2)?.concluida).toBe(true)
    expect(lerEntregaDoCache(cliente, HOJE, 99)).toBeUndefined()
  })

  it('conclui no cache do dia', () => {
    const cliente = clienteComInicio()

    aplicarConclusaoNoCache(cliente, HOJE, 1, true)

    expect(entrega(lido(cliente), 1).concluida).toBe(true)
  })

  it('restaura o estado exato de uma entrega concluída antes de ontem', () => {
    const cliente = clienteComInicio()
    const anterior = lerEntregaDoCache(cliente, HOJE, 2)
    if (anterior === undefined) throw new Error('entrega ausente')

    aplicarConclusaoNoCache(cliente, HOJE, 2, false)
    restaurarEntregaNoCache(cliente, HOJE, anterior)

    // A data original (ontem) volta; um simples "concluir de novo" a trocaria
    // pela de hoje.
    expect(entrega(lido(cliente), 2).concluidaData).toBe('2026-10-20')
  })

  it('restaurar uma entrega não desfaz a conclusão de outra em voo', () => {
    const cliente = clienteComInicio()
    const anterior = lerEntregaDoCache(cliente, HOJE, 1)
    if (anterior === undefined) throw new Error('entrega ausente')

    aplicarConclusaoNoCache(cliente, HOJE, 1, true)
    aplicarConclusaoNoCache(cliente, HOJE, 3, true)
    restaurarEntregaNoCache(cliente, HOJE, anterior)

    expect(entrega(lido(cliente), 1).concluida).toBe(false)
    expect(entrega(lido(cliente), 3).concluida).toBe(true)
  })

  it('não faz nada quando o dia ainda não está em cache', () => {
    const cliente = new QueryClient()

    aplicarConclusaoNoCache(cliente, HOJE, 1, true)

    expect(cliente.getQueryData(chavesInicio.dia(HOJE))).toBeUndefined()
  })

  it('não mexe no cache de outro dia', () => {
    const cliente = clienteComInicio()
    cliente.setQueryData(chavesInicio.dia('2026-10-20'), inicio())

    aplicarConclusaoNoCache(cliente, HOJE, 1, true)

    const outro = cliente.getQueryData<Inicio>(chavesInicio.dia('2026-10-20'))
    expect(outro && entrega(outro, 1).concluida).toBe(false)
  })
})
