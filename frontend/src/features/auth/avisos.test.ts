import { describe, expect, it } from 'vitest'
import { avisoDoEstado, textoDoAviso } from './avisos'

describe('avisoDoEstado', () => {
  it('lê os avisos que existem', () => {
    expect(avisoDoEstado({ aviso: 'senha-alterada' })).toBe('senha-alterada')
    expect(avisoDoEstado({ aviso: 'conta-excluida' })).toBe('conta-excluida')
  })

  it('ignora qualquer outro estado', () => {
    expect(avisoDoEstado(null)).toBeNull()
    expect(avisoDoEstado(undefined)).toBeNull()
    expect(avisoDoEstado({})).toBeNull()
    expect(avisoDoEstado({ aviso: 'qualquer-coisa' })).toBeNull()
    expect(avisoDoEstado('texto')).toBeNull()
  })
})

describe('textoDoAviso', () => {
  it('tem um texto para cada aviso', () => {
    expect(textoDoAviso('senha-alterada')).toContain('Senha alterada')
    expect(textoDoAviso('conta-excluida')).toContain('Conta excluída')
  })
})
