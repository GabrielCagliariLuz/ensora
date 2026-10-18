import { ErroApi } from '../../api/erro'

// Mensagens fixas por status: a do backend não é exibida, e a de credencial
// inválida é genérica de propósito (não revela se o e-mail existe).
export function mensagemErroLogin(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 400 || erro.status === 401) {
      return 'E-mail ou senha incorretos.'
    }
    if (erro.status === 429) {
      return 'Muitas tentativas. Tente de novo em alguns minutos.'
    }
    if (erro.status === 0) return 'Sem conexão com o servidor.'
  }
  return 'Não foi possível entrar. Tente de novo.'
}
