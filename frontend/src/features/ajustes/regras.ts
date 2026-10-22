import { ErroApi, errosPorCampo } from '../../api/erro'
import { erroDeNovaSenha } from '../auth/regrasCadastro'

export interface ValoresTrocaDeSenha {
  senhaAtual: string
  novaSenha: string
  confirmacao: string
}

export type CampoTrocaDeSenha = keyof ValoresTrocaDeSenha
export type ErrosTrocaDeSenha = Partial<Record<CampoTrocaDeSenha, string>>

export function validarTrocaDeSenha(
  valores: ValoresTrocaDeSenha,
): ErrosTrocaDeSenha {
  const erros: ErrosTrocaDeSenha = {}

  if (valores.senhaAtual === '') erros.senhaAtual = 'Informe a senha atual.'

  const nova = erroDeNovaSenha(valores.novaSenha)
  if (nova) erros.novaSenha = nova
  else if (valores.novaSenha === valores.senhaAtual) {
    erros.novaSenha = 'A nova senha deve ser diferente da atual.'
  } else if (valores.confirmacao !== valores.novaSenha) {
    erros.confirmacao = 'As senhas não conferem.'
  }
  return erros
}

// Campos do 400 que o formulário tem (senha atual errada, nova senha recusada).
export function errosDaTrocaDeSenha(erro: unknown): ErrosTrocaDeSenha {
  return errosPorCampo(erro, ['senhaAtual', 'novaSenha'] as const)
}

export function mensagemErroTrocaDeSenha(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 429) {
      return 'Muitas tentativas. Tente de novo em alguns minutos.'
    }
    if (erro.status === 400) return 'Confira as senhas.'
  }
  return 'Não foi possível trocar a senha. Tente de novo.'
}

export function mensagemErroFuso(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 400) return 'Fuso horário inválido.'
  }
  return 'Não foi possível salvar o fuso. Tente de novo.'
}

// A senha errada vem como 400 no campo "senha"; as demais falhas são gerais.
export function mensagemErroExclusao(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 429) {
      return 'Muitas tentativas. Tente de novo em alguns minutos.'
    }
    if (erro.status === 400) return 'Senha incorreta.'
  }
  return 'Não foi possível excluir a conta. Tente de novo.'
}
