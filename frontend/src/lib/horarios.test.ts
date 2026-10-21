import { describe, expect, it } from 'vitest'
import { ajustarHorario, normalizarHorario } from './horarios'

describe('normalizarHorario', () => {
  it('aceita HH:mm e completa com zero', () => {
    expect(normalizarHorario('09:30')).toBe('09:30')
    expect(normalizarHorario('9:30')).toBe('09:30')
    expect(normalizarHorario('23:59')).toBe('23:59')
    expect(normalizarHorario('00:00')).toBe('00:00')
  })

  it('aceita só dígitos, sem os dois pontos', () => {
    expect(normalizarHorario('0930')).toBe('09:30')
    expect(normalizarHorario('930')).toBe('09:30')
    expect(normalizarHorario('1430')).toBe('14:30')
  })

  it('aceita só a hora', () => {
    expect(normalizarHorario('9')).toBe('09:00')
    expect(normalizarHorario('14')).toBe('14:00')
  })

  it('ignora espaços nas pontas', () => {
    expect(normalizarHorario(' 9:30 ')).toBe('09:30')
  })

  it('devolve vazio para texto vazio', () => {
    expect(normalizarHorario('')).toBe('')
    expect(normalizarHorario('   ')).toBe('')
  })

  it('recusa horas e minutos fora do relógio', () => {
    expect(normalizarHorario('24:00')).toBeNull()
    expect(normalizarHorario('2400')).toBeNull()
    expect(normalizarHorario('24')).toBeNull()
    expect(normalizarHorario('09:60')).toBeNull()
    expect(normalizarHorario('0975')).toBeNull()
  })

  it('recusa texto que não é horário', () => {
    expect(normalizarHorario('abc')).toBeNull()
    expect(normalizarHorario('9:3')).toBeNull()
    expect(normalizarHorario('09:30:00')).toBeNull()
    expect(normalizarHorario('12345')).toBeNull()
    expect(normalizarHorario('-9:30')).toBeNull()
  })
})

describe('ajustarHorario', () => {
  const agora = new Date(2026, 9, 21, 10, 7)

  it('seta para cima soma 15 minutos', () => {
    expect(ajustarHorario('09:00', 1)).toBe('09:15')
    expect(ajustarHorario('09:45', 1)).toBe('10:00')
  })

  it('seta para baixo tira 15 minutos', () => {
    expect(ajustarHorario('09:00', -1)).toBe('08:45')
    expect(ajustarHorario('10:00', -1)).toBe('09:45')
  })

  it('vai para o próximo ou anterior múltiplo de 15 quando está fora dele', () => {
    expect(ajustarHorario('09:07', 1)).toBe('09:15')
    expect(ajustarHorario('09:07', -1)).toBe('09:00')
    expect(ajustarHorario('09:59', 1)).toBe('10:00')
    expect(ajustarHorario('09:01', -1)).toBe('09:00')
  })

  it('entende o texto ainda não normalizado', () => {
    expect(ajustarHorario('930', 1)).toBe('09:45')
    expect(ajustarHorario('9', -1)).toBe('08:45')
  })

  it('não passa de 00:00 nem de 23:45', () => {
    expect(ajustarHorario('00:00', -1)).toBe('00:00')
    expect(ajustarHorario('00:10', -1)).toBe('00:00')
    expect(ajustarHorario('23:45', 1)).toBe('23:45')
    expect(ajustarHorario('23:50', 1)).toBe('23:45')
  })

  it('vazio parte do quarto de hora mais próximo de agora', () => {
    expect(ajustarHorario('', 1, '', agora)).toBe('10:00')
    expect(ajustarHorario('', -1, '', agora)).toBe('10:00')
    expect(ajustarHorario('', 1, '', new Date(2026, 9, 21, 10, 8))).toBe('10:15')
  })

  it('vazio parte do horário de referência quando há um', () => {
    expect(ajustarHorario('', 1, '09:00', agora)).toBe('09:15')
    expect(ajustarHorario('', -1, '09:00', agora)).toBe('08:45')
  })

  it('ignora a referência inválida e usa agora', () => {
    expect(ajustarHorario('', 1, 'abc', agora)).toBe('10:00')
  })

  it('texto inválido também parte da referência ou de agora', () => {
    expect(ajustarHorario('xx', 1, '', agora)).toBe('10:00')
  })
})
