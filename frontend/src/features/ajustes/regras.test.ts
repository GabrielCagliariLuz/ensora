import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import {
  errosDaTrocaDeSenha,
  mensagemErroExclusao,
  mensagemErroFuso,
  mensagemErroTrocaDeSenha,
  validarTrocaDeSenha,
} from './regras'

const valida = {
  senhaAtual: 'senha-antiga',
  novaSenha: 'senha-nova-123',
  confirmacao: 'senha-nova-123',
}

describe('validarTrocaDeSenha', () => {
  it('troca completa não tem erro', () => {
    expect(validarTrocaDeSenha(valida)).toEqual({})
  })

  it('exige a senha atual', () => {
    expect(validarTrocaDeSenha({ ...valida, senhaAtual: '' }).senhaAtual).toBe(
      'Informe a senha atual.',
    )
  })

  it('a nova senha tem de 8 a 72 caracteres', () => {
    const curta = validarTrocaDeSenha({ ...valida, novaSenha: '1234567' })

    expect(curta.novaSenha).toBe('A senha deve ter pelo menos 8 caracteres.')
    expect(
      validarTrocaDeSenha({ ...valida, novaSenha: 'a'.repeat(73) }).novaSenha,
    ).toBe('A senha pode ter até 72 caracteres.')
  })

  it('a nova senha tem de ser diferente da atual', () => {
    const erros = validarTrocaDeSenha({
      senhaAtual: 'mesma-senha-1',
      novaSenha: 'mesma-senha-1',
      confirmacao: 'mesma-senha-1',
    })

    expect(erros.novaSenha).toBe('A nova senha deve ser diferente da atual.')
  })

  it('a confirmação tem de repetir a nova senha', () => {
    const erros = validarTrocaDeSenha({ ...valida, confirmacao: 'outra-coisa' })

    expect(erros.confirmacao).toBe('As senhas não conferem.')
    expect(erros.novaSenha).toBeUndefined()
  })
})

describe('errosDaTrocaDeSenha', () => {
  it('pega a senha atual errada e a nova recusada pelo servidor', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'inválido', [
      { campo: 'senhaAtual', mensagem: 'senha atual incorreta' },
      { campo: 'novaSenha', mensagem: 'a nova senha deve ser diferente da atual' },
    ])

    expect(errosDaTrocaDeSenha(erro)).toEqual({
      senhaAtual: 'senha atual incorreta',
      novaSenha: 'a nova senha deve ser diferente da atual',
    })
  })

  it('outros erros não têm campos', () => {
    expect(errosDaTrocaDeSenha(new ErroApi(429, 'LIMITE', 'x'))).toEqual({})
  })
})

describe('mensagens de erro', () => {
  it('troca de senha: rede, limite e geral', () => {
    expect(mensagemErroTrocaDeSenha(new ErroApi(0, 'REDE', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
    expect(mensagemErroTrocaDeSenha(new ErroApi(429, 'LIMITE', 'x'))).toBe(
      'Muitas tentativas. Tente de novo em alguns minutos.',
    )
    expect(mensagemErroTrocaDeSenha(new Error('x'))).toBe(
      'Não foi possível trocar a senha. Tente de novo.',
    )
  })

  it('fuso: inválido, rede e geral', () => {
    expect(mensagemErroFuso(new ErroApi(400, 'VALIDACAO', 'x'))).toBe(
      'Fuso horário inválido.',
    )
    expect(mensagemErroFuso(new ErroApi(0, 'REDE', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
    expect(mensagemErroFuso(new Error('x'))).toBe(
      'Não foi possível salvar o fuso. Tente de novo.',
    )
  })

  it('exclusão: senha incorreta é o 400, e os demais são gerais', () => {
    expect(mensagemErroExclusao(new ErroApi(400, 'VALIDACAO', 'x'))).toBe(
      'Senha incorreta.',
    )
    expect(mensagemErroExclusao(new ErroApi(429, 'LIMITE', 'x'))).toBe(
      'Muitas tentativas. Tente de novo em alguns minutos.',
    )
    expect(mensagemErroExclusao(new Error('x'))).toBe(
      'Não foi possível excluir a conta. Tente de novo.',
    )
  })
})
