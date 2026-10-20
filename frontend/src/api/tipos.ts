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
