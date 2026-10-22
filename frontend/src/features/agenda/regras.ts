import { ErroApi, errosPorCampo } from '../../api/erro'
import type {
  CategoriaEvento,
  Entrega,
  EntregaRequest,
  Evento,
  EventoRequest,
} from '../../api/tipos'
import { MENSAGEM_HORARIO_INVALIDO, normalizarHorario } from '../../lib/horarios'

export const TITULO_MAXIMO = 150
export const NOTA_MAXIMA = 1000
export const LOCAL_MAXIMO = 100

// O que o formulário guarda: textos. A data vem do campo nativo (AAAA-MM-DD) e
// o horário é o que a pessoa digitou (9:30, 0930...), normalizado para HH:mm
// só na hora de validar e enviar. Vazio quer dizer "não preenchido".
export interface ValoresEntrega {
  titulo: string
  prazoData: string
  prazoHorario: string
  nota: string
}

export interface ValoresEvento {
  titulo: string
  data: string
  inicio: string
  termino: string
  local: string
  categoria: CategoriaEvento | null
}

export const CAMPOS_ENTREGA = [
  'titulo',
  'prazoData',
  'prazoHorario',
  'nota',
] as const
export const CAMPOS_EVENTO = [
  'titulo',
  'data',
  'local',
  'inicio',
  'termino',
] as const

export type CampoEntrega = (typeof CAMPOS_ENTREGA)[number]
export type CampoEvento = (typeof CAMPOS_EVENTO)[number]
export type ErrosEntrega = Partial<Record<CampoEntrega, string>>
export type ErrosEvento = Partial<Record<CampoEvento, string>>

// O texto enviado é o aparado, então é o tamanho dele que conta.
function erroDeTitulo(titulo: string, rotulo: string): string | undefined {
  const aparado = titulo.trim()
  if (aparado === '') return `Informe o nome ${rotulo}.`
  if (aparado.length > TITULO_MAXIMO) {
    return `O nome pode ter até ${TITULO_MAXIMO} caracteres.`
  }
  return undefined
}

export function validarEntrega(valores: ValoresEntrega): ErrosEntrega {
  const erros: ErrosEntrega = {}

  const titulo = erroDeTitulo(valores.titulo, 'da entrega')
  if (titulo) erros.titulo = titulo
  if (valores.prazoData === '') erros.prazoData = 'Informe a data limite.'
  if (normalizarHorario(valores.prazoHorario) === null) {
    erros.prazoHorario = MENSAGEM_HORARIO_INVALIDO
  }
  if (valores.nota.trim().length > NOTA_MAXIMA) {
    erros.nota = `A nota pode ter até ${NOTA_MAXIMA} caracteres.`
  }
  return erros
}

export function validarEvento(valores: ValoresEvento): ErrosEvento {
  const erros: ErrosEvento = {}

  const titulo = erroDeTitulo(valores.titulo, 'do evento')
  if (titulo) erros.titulo = titulo
  if (valores.data === '') erros.data = 'Informe a data.'
  if (valores.local.trim().length > LOCAL_MAXIMO) {
    erros.local = `O local pode ter até ${LOCAL_MAXIMO} caracteres.`
  }

  const inicio = normalizarHorario(valores.inicio)
  const termino = normalizarHorario(valores.termino)
  if (inicio === '') erros.inicio = 'Informe o horário de início.'
  else if (inicio === null) erros.inicio = MENSAGEM_HORARIO_INVALIDO

  if (termino === null) {
    erros.termino = MENSAGEM_HORARIO_INVALIDO
    // HH:mm com zero à esquerda: comparar como texto equivale a comparar horas.
  } else if (termino !== '' && inicio && termino <= inicio) {
    erros.termino = 'O término deve ser depois do início.'
  }
  return erros
}

// Só vai o que foi preenchido: o backend trata ausente como "sem horário",
// "sem nota", "sem término", "sem local" e "sem categoria".
export function montarEntrega(valores: ValoresEntrega): EntregaRequest {
  const corpo: EntregaRequest = {
    titulo: valores.titulo.trim(),
    prazoData: valores.prazoData,
  }
  const prazoHorario = normalizarHorario(valores.prazoHorario)
  if (prazoHorario) corpo.prazoHorario = prazoHorario
  const nota = valores.nota.trim()
  if (nota !== '') corpo.nota = nota
  return corpo
}

export function montarEvento(valores: ValoresEvento): EventoRequest {
  const corpo: EventoRequest = {
    titulo: valores.titulo.trim(),
    data: valores.data,
    inicio: normalizarHorario(valores.inicio) ?? '',
  }
  const termino = normalizarHorario(valores.termino)
  if (termino) corpo.termino = termino
  const local = valores.local.trim()
  if (local !== '') corpo.local = local
  if (valores.categoria !== null) corpo.categoria = valores.categoria
  return corpo
}

// '09:00:00' vira '09:00': o servidor devolve com segundos e recebe sem.
function semSegundos(horario: string): string {
  return horario.slice(0, 5)
}

// Os dados atuais do evento como corpo de PUT, para trocar só o que mudou
// (ex.: mover para outro dia).
export function requisicaoDoEvento(
  evento: Evento,
  mudancas: Partial<EventoRequest> = {},
): EventoRequest {
  const corpo: EventoRequest = {
    titulo: evento.titulo,
    data: evento.data,
    inicio: semSegundos(evento.inicio),
  }
  if (evento.termino) corpo.termino = semSegundos(evento.termino)
  if (evento.local) corpo.local = evento.local
  if (evento.categoria) corpo.categoria = evento.categoria
  return { ...corpo, ...mudancas }
}

export function requisicaoDaEntrega(
  entrega: Entrega,
  mudancas: Partial<EntregaRequest> = {},
): EntregaRequest {
  const corpo: EntregaRequest = {
    titulo: entrega.titulo,
    prazoData: entrega.prazoData,
  }
  if (entrega.prazoHorario) corpo.prazoHorario = semSegundos(entrega.prazoHorario)
  if (entrega.nota) corpo.nota = entrega.nota
  return { ...corpo, ...mudancas }
}

export function errosDeCamposEntrega(erro: unknown): ErrosEntrega {
  return errosPorCampo(erro, CAMPOS_ENTREGA)
}

export function errosDeCamposEvento(erro: unknown): ErrosEvento {
  return errosPorCampo(erro, CAMPOS_EVENTO)
}

export function mensagemErroRegistro(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return 'Sem conexão com o servidor.'
    if (erro.status === 400) return 'Confira os dados do registro.'
  }
  return 'Não foi possível salvar. Tente de novo.'
}

export function mensagemErroMovimento(erro: unknown, titulo: string): string {
  if (erro instanceof ErroApi) {
    if (erro.status === 0) return `Sem conexão. "${titulo}" não foi movido.`
    if (erro.status === 404) return `"${titulo}" não existe mais.`
  }
  return `Não foi possível mover "${titulo}". Tente de novo.`
}
