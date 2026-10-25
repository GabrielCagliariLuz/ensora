import { requisitar } from './cliente'
import type { Conta } from './tipos'

export function carregarConta(sinal?: AbortSignal): Promise<Conta> {
  return requisitar<Conta>('/usuario', { sinal })
}

export function alterarFuso(fuso: string): Promise<Conta> {
  return requisitar<Conta>('/usuario/fuso', { metodo: 'PUT', corpo: { fuso } })
}

// Marca o tutorial de primeiro acesso como visto. Idempotente.
export function concluirIntroducao(): Promise<Conta> {
  return requisitar<Conta>('/usuario/introducao', { metodo: 'PUT' })
}

// Encerra todas as sessões no servidor: quem usa leva a pessoa de volta ao login.
export function alterarSenha(
  senhaAtual: string,
  novaSenha: string,
): Promise<void> {
  return requisitar<void>('/usuario/senha', {
    metodo: 'PUT',
    corpo: { senhaAtual, novaSenha },
  })
}

// Apaga a conta e tudo o que é dela. Não tem volta; a senha confirma.
export function excluirConta(senha: string): Promise<void> {
  return requisitar<void>('/usuario', { metodo: 'DELETE', corpo: { senha } })
}
