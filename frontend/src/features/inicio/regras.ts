import { ErroApi } from '../../api/erro'

export interface ProgressoSemanal {
  feitos: number
  meta: number
}

// Contador do hábito semanal no cartão. A meta pode ser ultrapassada, então
// 4/3 é um valor válido e não é limitado.
// Aviso quando a marcação não foi salva e o cartão voltou ao estado anterior.
export function mensagemErroMarcacao(erro: unknown, nome: string): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return `Sem conexão. "${nome}" não foi salvo.`
    if (erro.status === 404) return `"${nome}" não existe mais.`
  }
  return `Não foi possível salvar "${nome}". Tente de novo.`
}

// Aviso quando concluir ou desfazer a entrega não foi salvo e ela voltou ao
// estado anterior.
export function mensagemErroConclusao(erro: unknown, titulo: string): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return `Sem conexão. "${titulo}" não foi atualizada.`
    if (erro.status === 404) return `"${titulo}" não existe mais.`
  }
  return `Não foi possível atualizar "${titulo}". Tente de novo.`
}

export function textoProgresso({ feitos, meta }: ProgressoSemanal): string {
  return `${feitos}/${meta}`
}

// Atalhos da Home vazia: cada um abre o formulário de hábito com o nome pronto.
export const EXEMPLOS_DE_HABITO: readonly string[] = [
  'Ler',
  'Academia',
  'Beber água',
  'Meditação',
  'Estudar',
]
