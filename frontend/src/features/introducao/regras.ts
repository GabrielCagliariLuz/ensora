import type { Conta } from '../../api/tipos'

// Regras do tutorial de primeiro acesso, sem React: o texto de cada passo, a
// navegação entre eles e quando o tutorial aparece.

export type IdPasso = 'BOAS_VINDAS' | 'HABITOS' | 'ENTREGAS_E_EVENTOS' | 'INSIGHTS'

export interface Passo {
  id: IdPasso
  titulo: string
  texto: string
}

export const PASSOS: readonly Passo[] = [
  {
    id: 'BOAS_VINDAS',
    titulo: 'Bem-vindo ao Ensora',
    texto:
      'Um lugar só para o que você repete e para o que tem data: hábitos, entregas e eventos. Em quatro passos você entende como cada um funciona.',
  },
  {
    id: 'HABITOS',
    titulo: 'Hábitos',
    texto:
      'Repetem todo dia, ou algumas vezes por semana. Você marca na Home e o Ensora conta o que foi feito.',
  },
  {
    id: 'ENTREGAS_E_EVENTOS',
    titulo: 'Entregas e Eventos',
    texto:
      'Entrega tem prazo e você conclui. Evento tem dia e hora e fica só como lembrete, sem nada para marcar.',
  },
  {
    id: 'INSIGHTS',
    titulo: 'Insights',
    texto:
      'Mostra sua consistência: quantos dias você fez o que planejou. Os números aparecem depois de alguns dias de registro.',
  },
]

export const TOTAL_PASSOS = PASSOS.length

export function ehPrimeiroPasso(indice: number): boolean {
  return indice <= 0
}

export function ehUltimoPasso(indice: number): boolean {
  return indice >= TOTAL_PASSOS - 1
}

// Os dois param nas pontas: Continuar no último passo não avança, e Voltar no
// primeiro não existe na tela.
export function proximoPasso(indice: number): number {
  return Math.min(indice + 1, TOTAL_PASSOS - 1)
}

export function passoAnterior(indice: number): number {
  return Math.max(indice - 1, 0)
}

export function rotuloAvancar(indice: number): string {
  return ehUltimoPasso(indice) ? 'Começar' : 'Continuar'
}

// "1 DE 4": o índice começa em zero, a contagem mostrada em um.
export function rotuloContagem(indice: number): string {
  return `${indice + 1} DE ${TOTAL_PASSOS}`
}

// Só abre com a conta carregada: sem ela (carregando ou com erro) não dá para
// saber se a pessoa já viu, e abrir de novo seria pior do que esperar.
// "dispensada" cobre o intervalo entre fechar e a resposta do servidor.
export function deveMostrarIntroducao(
  conta: Conta | undefined,
  dispensada: boolean,
): boolean {
  return conta !== undefined && !conta.introducaoConcluida && !dispensada
}
