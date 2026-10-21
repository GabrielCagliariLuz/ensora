import { describe, expect, it } from 'vitest'
import type { Entrega } from '../../api/tipos'
import { agruparPorDia } from './agenda'
import { prazosDeHoje } from './prazosDeHoje'

const HOJE = '2026-10-21'
const MES = '2026-10'

function entrega(id: number, parcial: Partial<Entrega> = {}): Entrega {
  return {
    id,
    titulo: `E${id}`,
    prazoData: HOJE,
    concluida: false,
    ...parcial,
  }
}

function ids(entregas: Entrega[]): number[] {
  return entregas.map((e) => e.id)
}

function dias(entregas: Entrega[]) {
  return agruparPorDia({ entregas, eventos: [] })
}

describe('prazosDeHoje', () => {
  it('traz as entregas com prazo hoje, por horário', () => {
    const lista = [
      entrega(1, { prazoHorario: '18:00:00' }),
      entrega(2, { prazoHorario: '09:00:00' }),
      entrega(3),
    ]

    expect(ids(prazosDeHoje(dias(lista), MES, HOJE))).toEqual([2, 1, 3])
  })

  it('inclui as atrasadas pendentes, antes das de hoje', () => {
    const lista = [entrega(1), entrega(2, { prazoData: '2026-10-19' })]

    expect(ids(prazosDeHoje(dias(lista), MES, HOJE))).toEqual([2, 1])
  })

  it('não inclui atrasada já concluída', () => {
    const lista = [entrega(1, { prazoData: '2026-10-19', concluida: true })]

    expect(prazosDeHoje(dias(lista), MES, HOJE)).toEqual([])
  })

  it('entrega de hoje concluída fica por último', () => {
    const lista = [entrega(1, { concluida: true }), entrega(2)]

    expect(ids(prazosDeHoje(dias(lista), MES, HOJE))).toEqual([2, 1])
  })

  it('não inclui prazos futuros', () => {
    const lista = [entrega(1, { prazoData: '2026-10-22' })]

    expect(prazosDeHoje(dias(lista), MES, HOJE)).toEqual([])
  })

  it('devolve vazio quando hoje está fora da grade do mês visível', () => {
    const lista = [entrega(1, { prazoData: '2026-12-10' })]

    expect(prazosDeHoje(dias(lista), '2026-12', HOJE)).toEqual([])
  })
})
