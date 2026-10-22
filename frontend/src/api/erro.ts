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

// Os campos do 400 VALIDACAO que existem no formulário. Os demais não têm onde
// aparecer e caem na mensagem geral de quem chama.
export function errosPorCampo<C extends string>(
  erro: unknown,
  campos: readonly C[],
): Partial<Record<C, string>> {
  const erros: Partial<Record<C, string>> = {}
  if (!(erro instanceof ErroApi) || erro.status !== 400) return erros

  for (const { campo, mensagem } of erro.campos ?? []) {
    const conhecido = campos.find((nome) => nome === campo)
    if (conhecido !== undefined) erros[conhecido] = mensagem
  }
  return erros
}
