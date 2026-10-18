export interface LoginResponse {
  tokenAcesso: string
  expiraEmSegundos: number
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
