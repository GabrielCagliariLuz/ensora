import { describe, expect, it } from 'vitest'
import { ErroApi } from '../../api/erro'
import type { Entrega, Evento } from '../../api/tipos'
import {
  errosDeCamposEntrega,
  errosDeCamposEvento,
  mensagemErroRegistro,
  montarEntrega,
  montarEvento,
  requisicaoDaEntrega,
  requisicaoDoEvento,
  validarEntrega,
  validarEvento,
} from './regras'
import type { ValoresEntrega, ValoresEvento } from './regras'

const entrega: ValoresEntrega = {
  titulo: 'Trabalho de Cálculo',
  prazoData: '2026-11-05',
  prazoHorario: '',
  nota: '',
}

const evento: ValoresEvento = {
  titulo: 'Consulta',
  data: '2026-11-05',
  inicio: '09:00',
  termino: '',
  local: '',
  categoria: null,
}

describe('validarEntrega', () => {
  it('aceita só nome e data limite', () => {
    expect(validarEntrega(entrega)).toEqual({})
  })

  it('aceita data limite passada', () => {
    expect(validarEntrega({ ...entrega, prazoData: '2020-01-01' })).toEqual({})
  })

  it('recusa nome vazio ou só com espaços', () => {
    expect(validarEntrega({ ...entrega, titulo: '' }).titulo).toBe(
      'Informe o nome da entrega.',
    )
    expect(validarEntrega({ ...entrega, titulo: '   ' }).titulo).toBe(
      'Informe o nome da entrega.',
    )
  })

  it('aceita 150 caracteres e recusa 151, contando depois de aparar', () => {
    expect(validarEntrega({ ...entrega, titulo: 'a'.repeat(150) })).toEqual({})
    expect(validarEntrega({ ...entrega, titulo: ` ${'a'.repeat(150)} ` })).toEqual(
      {},
    )
    expect(validarEntrega({ ...entrega, titulo: 'a'.repeat(151) }).titulo).toBe(
      'O nome pode ter até 150 caracteres.',
    )
  })

  it('exige a data limite', () => {
    expect(validarEntrega({ ...entrega, prazoData: '' }).prazoData).toBe(
      'Informe a data limite.',
    )
  })

  it('aceita nota de 1000 caracteres e recusa 1001', () => {
    expect(validarEntrega({ ...entrega, nota: 'a'.repeat(1000) })).toEqual({})
    expect(validarEntrega({ ...entrega, nota: 'a'.repeat(1001) }).nota).toBe(
      'A nota pode ter até 1000 caracteres.',
    )
  })

  it('não exige o horário limite', () => {
    expect(validarEntrega({ ...entrega, prazoHorario: '' })).toEqual({})
  })

  it('aceita o horário limite em formatos curtos', () => {
    expect(validarEntrega({ ...entrega, prazoHorario: '930' })).toEqual({})
    expect(validarEntrega({ ...entrega, prazoHorario: '9:30' })).toEqual({})
  })

  it('recusa horário limite que não é horário', () => {
    const mensagem = 'Horário inválido. Use o formato 09:30.'

    expect(validarEntrega({ ...entrega, prazoHorario: '25:00' }).prazoHorario).toBe(
      mensagem,
    )
    expect(validarEntrega({ ...entrega, prazoHorario: 'abc' }).prazoHorario).toBe(
      mensagem,
    )
  })
})

describe('validarEvento', () => {
  it('aceita só nome, data e início', () => {
    expect(validarEvento(evento)).toEqual({})
  })

  it('recusa nome vazio e acima de 150', () => {
    expect(validarEvento({ ...evento, titulo: ' ' }).titulo).toBe(
      'Informe o nome do evento.',
    )
    expect(validarEvento({ ...evento, titulo: 'a'.repeat(151) }).titulo).toBe(
      'O nome pode ter até 150 caracteres.',
    )
  })

  it('exige data e horário de início', () => {
    const erros = validarEvento({ ...evento, data: '', inicio: '' })

    expect(erros.data).toBe('Informe a data.')
    expect(erros.inicio).toBe('Informe o horário de início.')
  })

  it('aceita término depois do início', () => {
    expect(validarEvento({ ...evento, termino: '09:01' })).toEqual({})
    expect(validarEvento({ ...evento, termino: '23:59' })).toEqual({})
  })

  it('recusa término igual ou antes do início', () => {
    const mensagem = 'O término deve ser depois do início.'

    expect(validarEvento({ ...evento, termino: '09:00' }).termino).toBe(mensagem)
    expect(validarEvento({ ...evento, termino: '08:59' }).termino).toBe(mensagem)
  })

  it('entende horários digitados em formato curto', () => {
    expect(validarEvento({ ...evento, inicio: '900', termino: '10' })).toEqual({})
    expect(validarEvento({ ...evento, inicio: '9:00', termino: '9:30' })).toEqual({})
  })

  it('compara o término com o início depois de normalizar', () => {
    // '10' vira 10:00, que é antes de 10:30.
    expect(validarEvento({ ...evento, inicio: '1030', termino: '10' }).termino).toBe(
      'O término deve ser depois do início.',
    )
  })

  it('recusa início e término que não são horários', () => {
    const mensagem = 'Horário inválido. Use o formato 09:30.'
    const erros = validarEvento({ ...evento, inicio: '99:99', termino: 'xx' })

    expect(erros.inicio).toBe(mensagem)
    expect(erros.termino).toBe(mensagem)
  })

  it('não cobra o término enquanto o início não foi preenchido', () => {
    const erros = validarEvento({ ...evento, inicio: '', termino: '08:00' })

    expect(erros.termino).toBeUndefined()
    expect(erros.inicio).toBeDefined()
  })

  it('aceita local de 100 caracteres e recusa 101', () => {
    expect(validarEvento({ ...evento, local: 'a'.repeat(100) })).toEqual({})
    expect(validarEvento({ ...evento, local: 'a'.repeat(101) }).local).toBe(
      'O local pode ter até 100 caracteres.',
    )
  })
})

describe('montarEntrega', () => {
  it('manda só o essencial quando o resto está vazio', () => {
    const corpo = montarEntrega(entrega)

    expect(corpo).toEqual({ titulo: 'Trabalho de Cálculo', prazoData: '2026-11-05' })
    expect('prazoHorario' in corpo).toBe(false)
    expect('nota' in corpo).toBe(false)
  })

  it('manda horário e nota quando preenchidos', () => {
    expect(
      montarEntrega({ ...entrega, prazoHorario: '14:30', nota: 'Cap. 1 a 4' }),
    ).toEqual({
      titulo: 'Trabalho de Cálculo',
      prazoData: '2026-11-05',
      prazoHorario: '14:30',
      nota: 'Cap. 1 a 4',
    })
  })

  it('manda o horário limite sempre em HH:mm', () => {
    expect(montarEntrega({ ...entrega, prazoHorario: '930' }).prazoHorario).toBe(
      '09:30',
    )
  })

  it('apara espaços e trata nota só com espaços como ausente', () => {
    const corpo = montarEntrega({ ...entrega, titulo: '  Lista  ', nota: '   ' })

    expect(corpo.titulo).toBe('Lista')
    expect('nota' in corpo).toBe(false)
  })
})

describe('montarEvento', () => {
  it('manda só o essencial quando o resto está vazio', () => {
    const corpo = montarEvento(evento)

    expect(corpo).toEqual({ titulo: 'Consulta', data: '2026-11-05', inicio: '09:00' })
    expect('termino' in corpo).toBe(false)
    expect('local' in corpo).toBe(false)
    expect('categoria' in corpo).toBe(false)
  })

  it('manda término, local e categoria quando preenchidos', () => {
    expect(
      montarEvento({
        ...evento,
        termino: '10:00',
        local: 'Clínica',
        categoria: 'SAUDE',
      }),
    ).toEqual({
      titulo: 'Consulta',
      data: '2026-11-05',
      inicio: '09:00',
      termino: '10:00',
      local: 'Clínica',
      categoria: 'SAUDE',
    })
  })

  it('manda início e término sempre em HH:mm', () => {
    const corpo = montarEvento({ ...evento, inicio: '9', termino: '1015' })

    expect(corpo.inicio).toBe('09:00')
    expect(corpo.termino).toBe('10:15')
  })

  it('apara espaços e trata local só com espaços como ausente', () => {
    const corpo = montarEvento({ ...evento, titulo: ' Consulta ', local: '  ' })

    expect(corpo.titulo).toBe('Consulta')
    expect('local' in corpo).toBe(false)
  })
})

describe('requisicaoDoEvento', () => {
  const completo: Evento = {
    id: 1,
    titulo: 'Consulta',
    data: '2026-10-05',
    inicio: '09:00:00',
    termino: '10:30:00',
    local: 'Clínica',
    categoria: 'SAUDE',
  }

  it('devolve o evento como corpo de PUT, com horários sem segundos', () => {
    expect(requisicaoDoEvento(completo)).toEqual({
      titulo: 'Consulta',
      data: '2026-10-05',
      inicio: '09:00',
      termino: '10:30',
      local: 'Clínica',
      categoria: 'SAUDE',
    })
  })

  it('omite o que o evento não tem', () => {
    const corpo = requisicaoDoEvento({
      id: 2,
      titulo: 'Simples',
      data: '2026-10-05',
      inicio: '09:00:00',
    })

    expect(corpo).toEqual({
      titulo: 'Simples',
      data: '2026-10-05',
      inicio: '09:00',
    })
  })

  it('troca só o que mudou', () => {
    expect(requisicaoDoEvento(completo, { data: '2026-10-07' })).toEqual({
      titulo: 'Consulta',
      data: '2026-10-07',
      inicio: '09:00',
      termino: '10:30',
      local: 'Clínica',
      categoria: 'SAUDE',
    })
  })
})

describe('requisicaoDaEntrega', () => {
  const completa: Entrega = {
    id: 1,
    titulo: 'Relatório',
    prazoData: '2026-10-20',
    prazoHorario: '18:00:00',
    concluida: true,
    concluidaData: '2026-10-19',
    nota: 'Capítulo 2',
  }

  it('devolve a entrega como corpo de PUT, sem a conclusão', () => {
    expect(requisicaoDaEntrega(completa)).toEqual({
      titulo: 'Relatório',
      prazoData: '2026-10-20',
      prazoHorario: '18:00',
      nota: 'Capítulo 2',
    })
  })

  it('omite horário e nota quando não existem', () => {
    const corpo = requisicaoDaEntrega({
      id: 2,
      titulo: 'Sem extras',
      prazoData: '2026-10-20',
      concluida: false,
    })

    expect(corpo).toEqual({ titulo: 'Sem extras', prazoData: '2026-10-20' })
  })

  it('troca só o que mudou', () => {
    expect(requisicaoDaEntrega(completa, { prazoData: '2026-10-22' })).toEqual({
      titulo: 'Relatório',
      prazoData: '2026-10-22',
      prazoHorario: '18:00',
      nota: 'Capítulo 2',
    })
  })
})

describe('erros do servidor', () => {
  it('mapeia os campos do 400 que a entrega tem', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'Dados inválidos', [
      { campo: 'titulo', mensagem: 'não pode estar em branco' },
      { campo: 'prazoData', mensagem: 'obrigatório' },
      { campo: 'inicio', mensagem: 'não existe na entrega' },
    ])

    expect(errosDeCamposEntrega(erro)).toEqual({
      titulo: 'não pode estar em branco',
      prazoData: 'obrigatório',
    })
  })

  it('mapeia os campos do 400 que o evento tem', () => {
    const erro = new ErroApi(400, 'VALIDACAO', 'Dados inválidos', [
      { campo: 'termino', mensagem: 'deve ser depois do início' },
      { campo: 'categoria', mensagem: 'sem campo no formulário' },
    ])

    expect(errosDeCamposEvento(erro)).toEqual({
      termino: 'deve ser depois do início',
    })
  })

  it('devolve vazio para outros erros', () => {
    expect(errosDeCamposEvento(new ErroApi(500, 'ERRO_INTERNO', 'x'))).toEqual({})
    expect(errosDeCamposEntrega(new Error('boom'))).toEqual({})
  })
})

describe('mensagemErroRegistro', () => {
  it('trata falha de rede', () => {
    expect(mensagemErroRegistro(new ErroApi(0, 'SEM_CONEXAO', 'x'))).toBe(
      'Sem conexão com o servidor.',
    )
  })

  it('trata validação sem campo conhecido (400)', () => {
    expect(mensagemErroRegistro(new ErroApi(400, 'VALIDACAO', 'x'))).toBe(
      'Confira os dados do registro.',
    )
  })

  it('usa a mensagem padrão para 5xx e erros desconhecidos', () => {
    const padrao = 'Não foi possível salvar. Tente de novo.'

    expect(mensagemErroRegistro(new ErroApi(500, 'ERRO_INTERNO', 'x'))).toBe(padrao)
    expect(mensagemErroRegistro(new Error('boom'))).toBe(padrao)
  })
})
