import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import {
  errosDeCampos,
  limitarMeta,
  mensagemErroExclusao,
  mensagemErroHabito,
  montarRequisicao,
  validarHabito,
} from './regras'

const diario = { nome: 'Beber água', tipo: 'DIARIO', vezesPorSemana: 3 } as const
const semanal = { nome: 'Academia', tipo: 'SEMANAL', vezesPorSemana: 3 } as const

describe('limitarMeta', () => {
  it('mantém valores de 1 a 7', () => {
    expect(limitarMeta(1)).toBe(1)
    expect(limitarMeta(4)).toBe(4)
    expect(limitarMeta(7)).toBe(7)
  })

  it('trava nos extremos', () => {
    expect(limitarMeta(0)).toBe(1)
    expect(limitarMeta(-3)).toBe(1)
    expect(limitarMeta(8)).toBe(7)
  })

  it('arredonda valores quebrados', () => {
    expect(limitarMeta(2.6)).toBe(3)
  })
})

describe('montarRequisicao', () => {
  it('diário não leva vezesPorSemana, mesmo com meta no estado do formulário', () => {
    const corpo = montarRequisicao(diario, '2026-10-21')

    expect(corpo).toEqual({
      nome: 'Beber água',
      tipo: 'DIARIO',
      data: '2026-10-21',
    })
    expect('vezesPorSemana' in corpo).toBe(false)
  })

  it('semanal leva a meta', () => {
    expect(montarRequisicao(semanal, '2026-10-21')).toEqual({
      nome: 'Academia',
      tipo: 'SEMANAL',
      vezesPorSemana: 3,
      data: '2026-10-21',
    })
  })

  it('apara espaços das pontas do nome', () => {
    expect(montarRequisicao({ ...diario, nome: '  Ler  ' }, '2026-10-21').nome).toBe(
      'Ler',
    )
  })

  it('mantém a meta do semanal dentro de 1 a 7', () => {
    expect(
      montarRequisicao({ ...semanal, vezesPorSemana: 9 }, '2026-10-21')
        .vezesPorSemana,
    ).toBe(7)
  })
})

describe('validarHabito', () => {
  it('aceita um nome válido', () => {
    expect(validarHabito(diario)).toEqual({})
  })

  it('recusa nome vazio ou só com espaços', () => {
    expect(validarHabito({ ...diario, nome: '' }).nome).toBe(
      'Informe o nome do hábito.',
    )
    expect(validarHabito({ ...diario, nome: '   ' }).nome).toBe(
      'Informe o nome do hábito.',
    )
  })

  it('aceita 100 caracteres e recusa 101', () => {
    expect(validarHabito({ ...diario, nome: 'a'.repeat(100) })).toEqual({})
    expect(validarHabito({ ...diario, nome: 'a'.repeat(101) }).nome).toBe(
      'O nome pode ter até 100 caracteres.',
    )
  })

  it('conta o tamanho depois de aparar', () => {
    expect(validarHabito({ ...diario, nome: ` ${'a'.repeat(100)} ` })).toEqual({})
  })
})

describe('errosDeCampos', () => {
  it('mapeia os campos do 400 que existem no formulário', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'Dados inválidos', [
      { campo: 'nome', mensagem: 'não pode estar em branco' },
      { campo: 'vezesPorSemana', mensagem: 'deve ser no máximo 7' },
    ])

    expect(errosDeCampos(erro)).toEqual({
      nome: 'não pode estar em branco',
      vezesPorSemana: 'deve ser no máximo 7',
    })
  })

  it('ignora campos que o formulário não tem', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'Dados inválidos', [
      { campo: 'data', mensagem: 'fora do intervalo' },
    ])

    expect(errosDeCampos(erro)).toEqual({})
  })

  it('devolve vazio para outros erros', () => {
    expect(errosDeCampos(new ErroApi(500, 'ERRO_INTERNO', 'x'))).toEqual({})
    expect(errosDeCampos(new Error('boom'))).toEqual({})
  })
})

describe('mensagemErroExclusao', () => {
  it('trata falha de rede', () => {
    expect(mensagemErroExclusao(new ErroApi(0, 'SEM_CONEXAO', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
  })

  it('usa a mensagem padrão para os demais erros', () => {
    const padrao = 'Não foi possível excluir. Tente de novo.'

    expect(mensagemErroExclusao(new ErroApi(500, 'ERRO_INTERNO', 'x'))).toBe(padrao)
    expect(mensagemErroExclusao(new Error('boom'))).toBe(padrao)
  })
})

describe('mensagemErroHabito', () => {
  it('trata falha de rede', () => {
    expect(mensagemErroHabito(new ErroApi(0, 'SEM_CONEXAO', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
  })

  it('trata hábito inexistente (404)', () => {
    expect(mensagemErroHabito(new ErroApi(404, 'NAO_ENCONTRADO', 'x'))).toBe(
      'Este hábito não existe mais.',
    )
  })

  it('trata validação sem campo conhecido (400)', () => {
    expect(mensagemErroHabito(new ErroApi(400, 'VALIDACAO', 'x'))).toBe(
      'Confira os dados do hábito.',
    )
  })

  it('usa a mensagem padrão para 5xx e erros desconhecidos', () => {
    const padrao = 'Não foi possível salvar. Tente de novo.'

    expect(mensagemErroHabito(new ErroApi(500, 'ERRO_INTERNO', 'x'))).toBe(padrao)
    expect(mensagemErroHabito(new Error('boom'))).toBe(padrao)
  })
})
