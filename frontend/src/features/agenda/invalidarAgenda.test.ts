import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { chavesInicio } from '../inicio/chaves'
import { invalidarDadosDeAgenda } from './invalidarAgenda'

function invalidada(cliente: QueryClient, chave: readonly unknown[]): boolean {
  return cliente.getQueryState(chave)?.isInvalidated === true
}

describe('invalidarDadosDeAgenda', () => {
  function clienteComDados(): QueryClient {
    const cliente = new QueryClient()
    cliente.setQueryData(chavesInicio.dia('2026-10-21'), { upcoming: {} })
    cliente.setQueryData(chavesInicio.dia('2026-10-20'), { upcoming: {} })
    cliente.setQueryData(['habitos'], [])
    return cliente
  }

  it('invalida a Home de qualquer dia em cache', async () => {
    const cliente = clienteComDados()

    await invalidarDadosDeAgenda(cliente)

    expect(invalidada(cliente, chavesInicio.dia('2026-10-21'))).toBe(true)
    expect(invalidada(cliente, chavesInicio.dia('2026-10-20'))).toBe(true)
  })

  it('não afeta consultas de outros assuntos', async () => {
    const cliente = clienteComDados()

    await invalidarDadosDeAgenda(cliente)

    expect(invalidada(cliente, ['habitos'])).toBe(false)
  })
})
