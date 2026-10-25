import { describe, expect, it } from 'vitest'
import type { Conta } from '../../api/tipos'
import {
  deveMostrarIntroducao,
  ehPrimeiroPasso,
  ehUltimoPasso,
  PASSOS,
  passoAnterior,
  proximoPasso,
  rotuloAvancar,
  rotuloContagem,
  TOTAL_PASSOS,
} from './regras'

const contaNova: Conta = {
  id: 1,
  email: 'a@b.com',
  fuso: 'America/Sao_Paulo',
  introducaoConcluida: false,
}

describe('passos', () => {
  it('são quatro, na ordem do tutorial', () => {
    expect(TOTAL_PASSOS).toBe(4)
    expect(PASSOS.map((passo) => passo.id)).toEqual([
      'BOAS_VINDAS',
      'HABITOS',
      'ENTREGAS_E_EVENTOS',
      'INSIGHTS',
    ])
  })
})

describe('navegação', () => {
  it('avança um passo por vez', () => {
    expect(proximoPasso(0)).toBe(1)
    expect(proximoPasso(2)).toBe(3)
  })

  it('não passa do último passo', () => {
    expect(proximoPasso(3)).toBe(3)
  })

  it('volta um passo por vez', () => {
    expect(passoAnterior(3)).toBe(2)
    expect(passoAnterior(1)).toBe(0)
  })

  it('não volta antes do primeiro passo', () => {
    expect(passoAnterior(0)).toBe(0)
  })

  it('reconhece as pontas', () => {
    expect(ehPrimeiroPasso(0)).toBe(true)
    expect(ehPrimeiroPasso(1)).toBe(false)
    expect(ehUltimoPasso(3)).toBe(true)
    expect(ehUltimoPasso(2)).toBe(false)
  })
})

describe('rótulos', () => {
  it('o botão diz Continuar e, no último passo, Começar', () => {
    expect(rotuloAvancar(0)).toBe('Continuar')
    expect(rotuloAvancar(2)).toBe('Continuar')
    expect(rotuloAvancar(3)).toBe('Começar')
  })

  it('a contagem mostra a posição em um', () => {
    expect(rotuloContagem(0)).toBe('1 DE 4')
    expect(rotuloContagem(3)).toBe('4 DE 4')
  })
})

describe('deveMostrarIntroducao', () => {
  it('abre para a conta que ainda não viu', () => {
    expect(deveMostrarIntroducao(contaNova, false)).toBe(true)
  })

  it('não abre para quem já concluiu', () => {
    expect(
      deveMostrarIntroducao({ ...contaNova, introducaoConcluida: true }, false),
    ).toBe(false)
  })

  it('não abre sem a conta carregada', () => {
    expect(deveMostrarIntroducao(undefined, false)).toBe(false)
  })

  it('não reabre depois de fechada, mesmo antes da resposta do servidor', () => {
    expect(deveMostrarIntroducao(contaNova, true)).toBe(false)
  })
})
