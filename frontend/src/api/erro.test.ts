import { describe, expect, it } from 'vitest'
import { ErroApi } from './erro'

describe('ErroApi', () => {
  it('carrega status, código, mensagem e campos', () => {
    const campos = [{ campo: 'nome', mensagem: 'não pode estar em branco' }]

    const erro = new ErroApi(400, 'VALIDACAO', 'Dados inválidos', campos)

    expect(erro).toBeInstanceOf(Error)
    expect(erro.name).toBe('ErroApi')
    expect(erro.status).toBe(400)
    expect(erro.erro).toBe('VALIDACAO')
    expect(erro.message).toBe('Dados inválidos')
    expect(erro.campos).toEqual(campos)
  })

  it('deixa campos indefinido quando não há', () => {
    const erro = new ErroApi(401, 'NAO_AUTENTICADO', 'Sessão inválida')

    expect(erro.campos).toBeUndefined()
  })
})
