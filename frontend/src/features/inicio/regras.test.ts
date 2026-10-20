import { describe, expect, it } from 'vitest'
import { textoProgresso } from './regras'

describe('textoProgresso', () => {
  it('mostra feitos/meta', () => {
    expect(textoProgresso({ feitos: 1, meta: 3 })).toBe('1/3')
    expect(textoProgresso({ feitos: 0, meta: 7 })).toBe('0/7')
  })

  it('mostra a meta batida', () => {
    expect(textoProgresso({ feitos: 3, meta: 3 })).toBe('3/3')
  })

  it('não limita quando a meta é ultrapassada', () => {
    expect(textoProgresso({ feitos: 4, meta: 3 })).toBe('4/3')
  })
})
