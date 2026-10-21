export interface LoginRequest {
  email: string
  senha: string
}

export interface LoginResponse {
  tokenAcesso: string
  expiraEmSegundos: number
}

export type TipoHabito = 'DIARIO' | 'SEMANAL'

export interface Habito {
  id: number
  nome: string
  tipo: TipoHabito
  // Só no semanal: a meta mais recente (a configurada).
  vezesPorSemana?: number
}

// Mesmo corpo para criar (POST) e editar (PUT). A data é o dia de hoje do
// aparelho (AAAA-MM-DD); vezesPorSemana só existe no semanal.
export interface HabitoRequest {
  nome: string
  tipo: TipoHabito
  vezesPorSemana?: number
  data: string
}

// Hábito na resposta de GET /inicio. feitos e meta só existem no semanal:
// feitos conta de segunda a domingo da semana da data, e meta é a que valia
// naquela semana.
export interface HabitoNaHome {
  id: number
  nome: string
  tipo: TipoHabito
  feito: boolean
  feitos?: number
  meta?: number
}

export interface Inicio {
  data: string
  habitos: HabitoNaHome[]
}

// Corpo de POST e PUT /entregas. prazoHorario vai em HH:mm; sem ele a entrega
// vale até 23:59. A conclusão não entra aqui: só o PATCH a altera.
export interface EntregaRequest {
  titulo: string
  prazoData: string
  prazoHorario?: string
  nota?: string
}

// Resposta de /entregas (e dos itens do upcoming do /inicio). Os horários
// voltam em HH:mm:ss, e os campos opcionais só aparecem quando existem.
export interface Entrega {
  id: number
  titulo: string
  prazoData: string
  prazoHorario?: string
  concluida: boolean
  concluidaEm?: string
  concluidaData?: string
  nota?: string
}

export type CategoriaEvento =
  | 'SAUDE'
  | 'TRABALHO'
  | 'PESSOAL'
  | 'ESTUDO'
  | 'CELEBRACAO'

// Corpo de POST e PUT /eventos. O PUT substitui o evento inteiro: o que vier
// ausente é limpo.
export interface EventoRequest {
  titulo: string
  data: string
  inicio: string
  termino?: string
  local?: string
  categoria?: CategoriaEvento
}

export interface Evento {
  id: number
  titulo: string
  data: string
  inicio: string
  termino?: string
  local?: string
  categoria?: CategoriaEvento
}

export interface CampoInvalido {
  campo: string
  mensagem: string
}

export interface ErroResposta {
  status: number
  erro: string
  mensagem: string
  caminho: string
  campos?: CampoInvalido[]
}
