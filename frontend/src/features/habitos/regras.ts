import { ErroApi } from '../../api/erro'
import type { HabitoRequest, TipoHabito } from '../../api/tipos'

export const META_MINIMA = 1
export const META_MAXIMA = 7
export const META_PADRAO = 3
export const NOME_MAXIMO = 100

export interface ValoresHabito {
  nome: string
  tipo: TipoHabito
  vezesPorSemana: number
}

export type CampoHabito = 'nome' | 'vezesPorSemana'
export type ErrosHabito = Partial<Record<CampoHabito, string>>

export function limitarMeta(valor: number): number {
  return Math.min(META_MAXIMA, Math.max(META_MINIMA, Math.round(valor)))
}

// Corpo comum de POST e PUT. No diário a meta é proibida pela API, então não
// vai; no semanal ela é obrigatória.
export function montarRequisicao(
  valores: ValoresHabito,
  hoje: string,
): HabitoRequest {
  const base = { nome: valores.nome.trim(), tipo: valores.tipo, data: hoje }
  if (valores.tipo === 'DIARIO') return base
  return { ...base, vezesPorSemana: limitarMeta(valores.vezesPorSemana) }
}

export function validarHabito(valores: ValoresHabito): ErrosHabito {
  const nome = valores.nome.trim()
  if (nome === '') return { nome: 'Informe o nome do hábito.' }
  if (nome.length > NOME_MAXIMO) {
    return { nome: `O nome pode ter até ${NOME_MAXIMO} caracteres.` }
  }
  return {}
}

// Campos do 400 VALIDACAO que existem no formulário. Os demais (tipo, data)
// não têm onde aparecer e caem na mensagem geral.
export function errosDeCampos(erro: unknown): ErrosHabito {
  const erros: ErrosHabito = {}
  if (!(erro instanceof ErroApi) || erro.status !== 400) return erros

  for (const { campo, mensagem } of erro.campos ?? []) {
    if (campo === 'nome' || campo === 'vezesPorSemana') erros[campo] = mensagem
  }
  return erros
}

export function mensagemErroExclusao(erro: unknown): string {
  if (erro instanceof ErroApi && erro.status === 0) {
    return 'Sem conexão com o servidor.'
  }
  return 'Não foi possível excluir. Tente de novo.'
}

export function mensagemErroHabito(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 404) return 'Este hábito não existe mais.'
    if (erro.status === 400) return 'Confira os dados do hábito.'
  }
  return 'Não foi possível salvar. Tente de novo.'
}
