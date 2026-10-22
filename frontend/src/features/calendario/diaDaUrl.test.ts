import { describe, expect, it } from 'vitest'
import { diaDaBusca, pedidoDaBusca } from './diaDaUrl'

describe('diaDaBusca', () => {
  it('aceita uma data que existe', () => {
    expect(diaDaBusca('2026-10-25')).toBe('2026-10-25')
    expect(diaDaBusca('2028-02-29')).toBe('2028-02-29')
  })

  it('ignora o que não é data', () => {
    expect(diaDaBusca(null)).toBeNull()
    expect(diaDaBusca('')).toBeNull()
    expect(diaDaBusca('hoje')).toBeNull()
    expect(diaDaBusca('2026-10')).toBeNull()
    expect(diaDaBusca('25/10/2026')).toBeNull()
  })

  it('ignora um dia que não existe no mês', () => {
    expect(diaDaBusca('2026-02-30')).toBeNull()
    expect(diaDaBusca('2027-02-29')).toBeNull()
    expect(diaDaBusca('2026-13-01')).toBeNull()
    expect(diaDaBusca('2026-00-10')).toBeNull()
  })
})

describe('pedidoDaBusca', () => {
  const busca = (texto: string) => new URLSearchParams(texto)

  it('devolve só o dia quando não há registro', () => {
    expect(pedidoDaBusca(busca('dia=2026-10-25'))).toEqual({
      dia: '2026-10-25',
      registro: null,
    })
  })

  it('devolve o registro quando tipo e id são válidos', () => {
    expect(pedidoDaBusca(busca('dia=2026-10-25&tipo=entrega&id=12'))).toEqual({
      dia: '2026-10-25',
      registro: { tipo: 'ENTREGA', id: 12 },
    })
    expect(pedidoDaBusca(busca('dia=2026-10-25&tipo=EVENTO&id=3'))?.registro).toEqual(
      { tipo: 'EVENTO', id: 3 },
    )
  })

  it('ignora o registro com tipo ou id inválido, mas mantém o dia', () => {
    for (const texto of [
      'dia=2026-10-25&tipo=habito&id=1',
      'dia=2026-10-25&tipo=entrega',
      'dia=2026-10-25&tipo=entrega&id=0',
      'dia=2026-10-25&tipo=entrega&id=1.5',
      'dia=2026-10-25&tipo=entrega&id=abc',
    ]) {
      expect(pedidoDaBusca(busca(texto))).toEqual({
        dia: '2026-10-25',
        registro: null,
      })
    }
  })

  it('ignora o pedido sem dia válido', () => {
    expect(pedidoDaBusca(busca(''))).toBeNull()
    expect(pedidoDaBusca(busca('tipo=entrega&id=1'))).toBeNull()
    expect(pedidoDaBusca(busca('dia=2026-02-30&tipo=entrega&id=1'))).toBeNull()
  })
})
