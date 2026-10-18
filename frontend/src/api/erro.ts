import type { CampoInvalido } from './tipos'

export class ErroApi extends Error {
  readonly status: number
  readonly erro: string
  readonly campos?: CampoInvalido[]

  constructor(
    status: number,
    erro: string,
    mensagem: string,
    campos?: CampoInvalido[],
  ) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.erro = erro
    this.campos = campos
  }
}
