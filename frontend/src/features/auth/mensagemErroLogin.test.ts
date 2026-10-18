import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import { mensagemErroLogin } from './mensagemErroLogin'

function erroApi(status: number): ErroApi {
  return new ErroApi(status, 'QUALQUER', 'mensagem do backend')
}

describe('mensagemErroLogin', () => {
  it('credencial inválida (401) usa a mensagem genérica', () => {
    expect(mensagemErroLogin(erroApi(401))).toBe('E-mail ou senha incorretos.')
  })

  it('validação (400) também usa a mensagem genérica', () => {
    expect(mensagemErroLogin(erroApi(400))).toBe('E-mail ou senha incorretos.')
  })

  it('limite de tentativas (429) pede para esperar', () => {
    expect(mensagemErroLogin(erroApi(429))).toBe(
      'Muitas tentativas. Tente de novo em alguns minutos.',
    )
  })

  it('falha de rede (status 0) avisa que está sem conexão', () => {
    expect(mensagemErroLogin(erroApi(0))).toBe('Sem conexão com o servidor.')
  })

  it('5xx e erros desconhecidos usam a mensagem padrão', () => {
    const padrao = 'Não foi possível entrar. Tente de novo.'

    expect(mensagemErroLogin(erroApi(500))).toBe(padrao)
    expect(mensagemErroLogin(new Error('boom'))).toBe(padrao)
  })

  it('nunca mostra a mensagem que veio do backend', () => {
    expect(mensagemErroLogin(erroApi(401))).not.toContain('backend')
  })
})
