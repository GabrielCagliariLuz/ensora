import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import { mensagemErroMarcacao, textoProgresso } from './regras'

describe('mensagemErroMarcacao', () => {
  it('avisa quando está sem conexão', () => {
    expect(mensagemErroMarcacao(new ErroApi(0, 'SEM_CONEXAO', 'x'), 'Ler')).toBe(
      'Sem conexão. "Ler" não foi salvo.',
    )
  })

  it('avisa quando o hábito não existe mais (404)', () => {
    expect(
      mensagemErroMarcacao(new ErroApi(404, 'NAO_ENCONTRADO', 'x'), 'Ler'),
    ).toBe('"Ler" não existe mais.')
  })

  it('usa a mensagem padrão para os demais erros', () => {
    const padrao = 'Não foi possível salvar "Ler". Tente de novo.'

    expect(mensagemErroMarcacao(new ErroApi(500, 'ERRO_INTERNO', 'x'), 'Ler')).toBe(
      padrao,
    )
    expect(mensagemErroMarcacao(new ErroApi(400, 'VALIDACAO', 'x'), 'Ler')).toBe(
      padrao,
    )
    expect(mensagemErroMarcacao(new Error('boom'), 'Ler')).toBe(padrao)
  })
})

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
