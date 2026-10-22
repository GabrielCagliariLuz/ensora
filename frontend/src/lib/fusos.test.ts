import { describe, expect, it } from 'vitest'
import { FUSOS_DO_BRASIL, fusoDoNavegador, gruposDeFuso } from './fusos'

describe('gruposDeFuso', () => {
  it('põe os fusos do Brasil no primeiro grupo, com o nome conhecido', () => {
    const { brasil } = gruposDeFuso('America/Sao_Paulo', [])

    expect(brasil).toHaveLength(Object.keys(FUSOS_DO_BRASIL).length)
    expect(brasil[0].valor).toBe('America/Noronha')
    expect(
      brasil.find((fuso) => fuso.valor === 'America/Rio_Branco')?.rotulo,
    ).toBe('Rio Branco (Acre)')
  })

  it('o segundo grupo não repete os do Brasil e vem em ordem alfabética', () => {
    const { outros } = gruposDeFuso('America/Sao_Paulo', [
      'Europe/Lisbon',
      'America/Sao_Paulo',
      'Asia/Tokyo',
      'America/New_York',
    ])

    expect(outros.map((fuso) => fuso.valor)).toEqual([
      'America/New_York',
      'Asia/Tokyo',
      'Europe/Lisbon',
    ])
  })

  it('troca o sublinhado por espaço no nome dos demais', () => {
    const { outros } = gruposDeFuso('America/Sao_Paulo', [
      'America/Argentina/Buenos_Aires',
    ])

    expect(outros[0].rotulo).toBe('America / Argentina / Buenos Aires')
  })

  it('inclui o fuso atual mesmo que o navegador não o liste', () => {
    const { outros } = gruposDeFuso('Pacific/Kiritimati', ['Asia/Tokyo'])

    expect(outros.map((fuso) => fuso.valor)).toContain('Pacific/Kiritimati')
  })

  it('um fuso atual do Brasil não vai para o segundo grupo', () => {
    const { outros } = gruposDeFuso('America/Manaus', [])

    expect(outros).toEqual([])
  })

  it('sem lista do navegador, devolve só o grupo do Brasil', () => {
    const { brasil, outros } = gruposDeFuso('America/Sao_Paulo', [])

    expect(brasil.length).toBeGreaterThan(0)
    expect(outros).toEqual([])
  })
})

describe('fusoDoNavegador', () => {
  it('devolve um nome de região', () => {
    expect(fusoDoNavegador()).toMatch(/^[A-Za-z_]+(\/[A-Za-z_+\-0-9]+)*$/)
  })
})
