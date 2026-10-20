import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { chavesInicio } from '../inicio/chaves'
import { chavesHabitos } from './chaves'
import { invalidarDadosDeHabitos } from './invalidarHabitos'

function invalidada(cliente: QueryClient, chave: readonly unknown[]): boolean {
  return cliente.getQueryState(chave)?.isInvalidated === true
}

describe('invalidarDadosDeHabitos', () => {
  function clienteComDados(): QueryClient {
    const cliente = new QueryClient()
    cliente.setQueryData(chavesHabitos.todos, [])
    cliente.setQueryData(chavesInicio.dia('2026-10-21'), { habitos: [] })
    cliente.setQueryData(chavesInicio.dia('2026-10-20'), { habitos: [] })
    cliente.setQueryData(['outra'], 'intacta')
    return cliente
  }

  it('invalida a lista de hábitos', async () => {
    const cliente = clienteComDados()

    await invalidarDadosDeHabitos(cliente)

    expect(invalidada(cliente, chavesHabitos.todos)).toBe(true)
  })

  it('invalida a Home de qualquer dia em cache', async () => {
    const cliente = clienteComDados()

    await invalidarDadosDeHabitos(cliente)

    expect(invalidada(cliente, chavesInicio.dia('2026-10-21'))).toBe(true)
    expect(invalidada(cliente, chavesInicio.dia('2026-10-20'))).toBe(true)
  })

  it('não afeta consultas de outros assuntos', async () => {
    const cliente = clienteComDados()

    await invalidarDadosDeHabitos(cliente)

    expect(invalidada(cliente, ['outra'])).toBe(false)
  })
})
