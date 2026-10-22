import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import {
  erroDeNovaSenha,
  errosDoCadastro,
  mensagemErroCadastro,
  validarCadastro,
} from './regrasCadastro'
import type { ValoresCadastro } from './regrasCadastro'

const valido: ValoresCadastro = {
  email: 'pessoa@exemplo.com',
  senha: 'senha-segura',
  confirmacao: 'senha-segura',
  codigoConvite: '',
}

describe('erroDeNovaSenha', () => {
  it('aceita de 8 a 72 caracteres', () => {
    expect(erroDeNovaSenha('12345678')).toBeUndefined()
    expect(erroDeNovaSenha('a'.repeat(72))).toBeUndefined()
  })

  it('recusa menos de 8 e mais de 72', () => {
    expect(erroDeNovaSenha('1234567')).toBe(
      'A senha deve ter pelo menos 8 caracteres.',
    )
    expect(erroDeNovaSenha('a'.repeat(73))).toBe(
      'A senha pode ter até 72 caracteres.',
    )
  })
})

describe('validarCadastro', () => {
  it('cadastro completo não tem erro', () => {
    expect(validarCadastro(valido, false)).toEqual({})
  })

  it('exige o e-mail e confere o formato', () => {
    expect(validarCadastro({ ...valido, email: '  ' }, false).email).toBe(
      'Informe o e-mail.',
    )
    expect(validarCadastro({ ...valido, email: 'sem-arroba' }, false).email).toBe(
      'E-mail inválido.',
    )
    expect(validarCadastro({ ...valido, email: 'a@b' }, false).email).toBe(
      'E-mail inválido.',
    )
  })

  it('confere o tamanho da senha antes de comparar a confirmação', () => {
    const erros = validarCadastro(
      { ...valido, senha: '123', confirmacao: '999' },
      false,
    )

    expect(erros.senha).toBe('A senha deve ter pelo menos 8 caracteres.')
    expect(erros.confirmacao).toBeUndefined()
  })

  it('pede que a confirmação seja igual à senha', () => {
    const erros = validarCadastro({ ...valido, confirmacao: 'outra-coisa' }, false)

    expect(erros.confirmacao).toBe('As senhas não conferem.')
  })

  it('só exige o código de convite quando o servidor pede', () => {
    expect(validarCadastro(valido, false).codigoConvite).toBeUndefined()
    expect(validarCadastro(valido, true).codigoConvite).toBe(
      'Informe o código de convite.',
    )
    expect(
      validarCadastro({ ...valido, codigoConvite: 'familia' }, true).codigoConvite,
    ).toBeUndefined()
  })
})

describe('errosDoCadastro', () => {
  it('pega só os campos que a tela tem', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'inválido', [
      { campo: 'senha', mensagem: 'muito curta' },
      { campo: 'codigoConvite', mensagem: 'Código de convite inválido' },
      { campo: 'fuso', mensagem: 'fuso horário inválido' },
    ])

    expect(errosDoCadastro(erro)).toEqual({
      senha: 'muito curta',
      codigoConvite: 'Código de convite inválido',
    })
  })

  it('outro erro não tem campos', () => {
    expect(errosDoCadastro(new ErroApi(409, 'CONFLITO', 'x'))).toEqual({})
    expect(errosDoCadastro(new Error('x'))).toEqual({})
  })
})

describe('mensagemErroCadastro', () => {
  it('traduz o status', () => {
    expect(mensagemErroCadastro(new ErroApi(403, 'ACESSO_NEGADO', 'x'))).toBe(
      'O cadastro está fechado no momento.',
    )
    expect(mensagemErroCadastro(new ErroApi(409, 'CONFLITO', 'x'))).toBe(
      'Este e-mail já está cadastrado.',
    )
    expect(mensagemErroCadastro(new ErroApi(429, 'LIMITE', 'x'))).toBe(
      'Muitas tentativas. Tente de novo em alguns minutos.',
    )
    expect(mensagemErroCadastro(new ErroApi(0, 'REDE', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
  })

  it('o resto cai na mensagem geral', () => {
    expect(mensagemErroCadastro(new Error('x'))).toBe(
      'Não foi possível criar a conta. Tente de novo.',
    )
  })
})
