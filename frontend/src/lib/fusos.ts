export const FUSO_PADRAO = 'America/Sao_Paulo'

// Os fusos do Brasil, com o nome que a pessoa reconhece (os nomes IANA usam
// cidades). Vêm primeiro na lista de Configurações.
export const FUSOS_DO_BRASIL: Record<string, string> = {
  'America/Noronha': 'Fernando de Noronha',
  'America/Sao_Paulo': 'Brasília (São Paulo, Rio, Belo Horizonte)',
  'America/Fortaleza': 'Fortaleza',
  'America/Recife': 'Recife',
  'America/Belem': 'Belém',
  'America/Bahia': 'Salvador (Bahia)',
  'America/Maceio': 'Maceió',
  'America/Araguaina': 'Araguaína (Tocantins)',
  'America/Santarem': 'Santarém',
  'America/Cuiaba': 'Cuiabá (Mato Grosso)',
  'America/Campo_Grande': 'Campo Grande (Mato Grosso do Sul)',
  'America/Porto_Velho': 'Porto Velho (Rondônia)',
  'America/Boa_Vista': 'Boa Vista (Roraima)',
  'America/Manaus': 'Manaus (Amazonas)',
  'America/Eirunepe': 'Eirunepé (Amazonas)',
  'America/Rio_Branco': 'Rio Branco (Acre)',
}

export interface OpcaoDeFuso {
  valor: string
  rotulo: string
}

export interface GruposDeFuso {
  brasil: OpcaoDeFuso[]
  outros: OpcaoDeFuso[]
}

// O fuso do aparelho, lido do navegador (nome IANA). Sem ele, o padrão do app.
export function fusoDoNavegador(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || FUSO_PADRAO
  } catch {
    return FUSO_PADRAO
  }
}

// Todos os fusos que o navegador conhece. Navegador sem suporte devolve vazio, e
// a lista de Configurações fica só com os do Brasil e o fuso atual.
export function todosOsFusos(): string[] {
  const intl = Intl as unknown as {
    supportedValuesOf?: (chave: string) => string[]
  }
  try {
    return intl.supportedValuesOf?.('timeZone') ?? []
  } catch {
    return []
  }
}

// 'America/Argentina/Buenos_Aires' vira 'America / Argentina / Buenos Aires'.
function rotuloDoNome(nome: string): string {
  return nome.replace(/_/g, ' ').replace(/\//g, ' / ')
}

// Os fusos em dois grupos: os do Brasil, com nome conhecido, e todos os demais
// em ordem alfabética. O fuso atual sempre aparece, mesmo que o navegador não o
// liste (ele foi gravado antes, por outro aparelho).
export function gruposDeFuso(atual: string, todos: string[]): GruposDeFuso {
  const brasil = Object.entries(FUSOS_DO_BRASIL).map(([valor, rotulo]) => ({
    valor,
    rotulo,
  }))

  const nomes = new Set(todos)
  if (atual !== '' && !(atual in FUSOS_DO_BRASIL)) nomes.add(atual)

  const outros = [...nomes]
    .filter((nome) => !(nome in FUSOS_DO_BRASIL))
    .sort((a, b) => a.localeCompare(b))
    .map((valor) => ({ valor, rotulo: rotuloDoNome(valor) }))

  return { brasil, outros }
}
