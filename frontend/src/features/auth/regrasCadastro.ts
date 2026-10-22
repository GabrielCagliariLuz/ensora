import { ErroApi, errosPorCampo } from '../../api/erro'

// Mesmos limites do backend (cadastro: senha de 8 a 72, o teto do BCrypt).
export const SENHA_MINIMA = 8
export const SENHA_MAXIMA = 72

export interface ValoresCadastro {
  email: string
  senha: string
  confirmacao: string
  codigoConvite: string
}

export type CampoCadastro = keyof ValoresCadastro
export type ErrosCadastro = Partial<Record<CampoCadastro, string>>

// Só o essencial de um e-mail; quem decide de verdade é o servidor.
const EMAIL_PARECE_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// A mensagem de tamanho da senha é a mesma do cadastro e da troca de senha.
export function erroDeNovaSenha(senha: string): string | undefined {
  if (senha.length < SENHA_MINIMA) {
    return `A senha deve ter pelo menos ${SENHA_MINIMA} caracteres.`
  }
  if (senha.length > SENHA_MAXIMA) {
    return `A senha pode ter até ${SENHA_MAXIMA} caracteres.`
  }
  return undefined
}

export function validarCadastro(
  valores: ValoresCadastro,
  exigeConvite: boolean,
): ErrosCadastro {
  const erros: ErrosCadastro = {}

  const email = valores.email.trim()
  if (email === '') erros.email = 'Informe o e-mail.'
  else if (!EMAIL_PARECE_VALIDO.test(email)) erros.email = 'E-mail inválido.'

  const senha = erroDeNovaSenha(valores.senha)
  if (senha) erros.senha = senha
  else if (valores.confirmacao !== valores.senha) {
    erros.confirmacao = 'As senhas não conferem.'
  }

  if (exigeConvite && valores.codigoConvite.trim() === '') {
    erros.codigoConvite = 'Informe o código de convite.'
  }
  return erros
}

// Campos do 400 que o formulário tem; o fuso não tem campo na tela.
export function errosDoCadastro(erro: unknown): ErrosCadastro {
  return errosPorCampo(erro, ['email', 'senha', 'codigoConvite'] as const)
}

// Mensagem geral, para o que não é de um campo.
export function mensagemErroCadastro(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 403) return 'O cadastro está fechado no momento.'
    if (erro.status === 409) return 'Este e-mail já está cadastrado.'
    if (erro.status === 429) {
      return 'Muitas tentativas. Tente de novo em alguns minutos.'
    }
    if (erro.status === 400) return 'Confira os dados do cadastro.'
  }
  return 'Não foi possível criar a conta. Tente de novo.'
}
