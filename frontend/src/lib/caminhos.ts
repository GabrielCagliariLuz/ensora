export const CAMINHOS = {
  inicio: '/',
  insights: '/insights',
  calendario: '/calendario',
  ajustes: '/ajustes',
  login: '/login',
  cadastro: '/cadastro',
} as const

// O Calendar aberto num dia ('AAAA-MM-DD') e, se informado, com os detalhes de um
// registro abertos: usado pelos cartões da Home.
export function calendarioNoDia(
  dia: string,
  registro?: { tipo: 'ENTREGA' | 'EVENTO'; id: number },
): string {
  const busca = new URLSearchParams({ dia })
  if (registro) {
    busca.set('tipo', registro.tipo.toLowerCase())
    busca.set('id', String(registro.id))
  }
  return `${CAMINHOS.calendario}?${busca.toString()}`
}
