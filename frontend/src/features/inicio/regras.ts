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

export function textoProgresso({ feitos, meta }: ProgressoSemanal): string {
  return `${feitos}/${meta}`
}
