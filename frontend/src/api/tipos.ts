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

// Resposta de GET /calendario: todas as entregas (concluídas incluídas) e todos
// os eventos de `de` a `ate`, as duas pontas inclusivas, em listas planas.
export interface Calendario {
  de: string
  ate: string
  entregas: Entrega[]
  eventos: Evento[]
}

// Entregas e eventos dos próximos dias, no mesmo formato de /entregas e
// /eventos. A ordem vem do servidor.
export interface Upcoming {
  entregas: Entrega[]
  eventos: Evento[]
}

export interface Inicio {
  data: string
  habitos: HabitoNaHome[]
  upcoming: Upcoming
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

// Estado de uma célula hábito × dia da grade de Insights (seção 7.4).
export type EstadoCelula =
  | 'CONCLUIDO'
  | 'DESCANSO_FUTURO'
  | 'NAO_REALIZADO'
  | 'EM_ABERTO'

export interface CelulaInsights {
  data: string
  estado: EstadoCelula
}

// Resumo de uma semana (segunda a domingo) de um hábito semanal.
export interface SemanaDoHabito {
  de: string
  ate: string
  meta: number
  feitos: number
  batida: boolean
  encerrada: boolean
  contaNaConsistencia: boolean
}

// Hábito na resposta de GET /insights/consistencia. `dias` só tem os dias em
// que o hábito existia; `semanas` só existe no semanal; `consistencia` (0 a 100)
// não aparece quando não há dias ou semanas a considerar.
export interface HabitoInsights {
  id: number
  nome: string
  tipo: TipoHabito
  excluido: boolean
  consistencia?: number
  semanas?: SemanaDoHabito[]
  dias: CelulaInsights[]
}

export interface Consistencia {
  mes: string
  habitos: HabitoInsights[]
}

// Ritmo de um dia, em percentual de 0 a 100 com uma casa decimal. Sem `ritmo`
// quando nenhum hábito existia no dia.
export interface DiaDoRitmo {
  data: string
  ritmo?: number
}

// Bloco semanal do ritmo: S1 a S4 (dias 1-7, 8-14, 15-21 e 22 em diante).
export interface SemanaDoRitmo {
  rotulo: string
  de: string
  ate: string
  ritmo?: number
}

// Resposta de GET /insights/ritmo (decisão 57).
export interface Ritmo {
  de: string
  ate: string
  ritmoMedio?: number
  semanas: SemanaDoRitmo[]
  dias: DiaDoRitmo[]
}

// Resposta de GET /usuario e de PUT /usuario/fuso: a conta de quem está logado.
export interface Conta {
  id: number
  email: string
  // Nome IANA do fuso (ex.: America/Sao_Paulo).
  fuso: string
}

// GET /auth/registro (público): o que a tela de cadastro precisa saber.
export interface ConfiguracaoCadastro {
  habilitado: boolean
  exigeConvite: boolean
}

// Corpo de POST /auth/registrar. O código só vale quando o servidor exige convite.
export interface CadastroRequest {
  email: string
  senha: string
  codigoConvite?: string
  fuso?: string
}
