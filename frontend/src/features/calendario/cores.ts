import type { Entrega } from '../../api/tipos'
import { corDaCategoria } from '../agenda/categorias'
import { situacaoPrazo } from '../inicio/upcoming'
import type { Registro } from './agenda'

// Entrega pendente e crítica (atrasada, hoje ou amanhã): a única que fica
// vermelha. A concluída nunca é urgente.
export function entregaUrgente(entrega: Entrega, agora: Date): boolean {
  return !entrega.concluida && situacaoPrazo(entrega, agora).urgente
}

// Token de cor do registro: a categoria do evento (neutra sem categoria), ember
// para a entrega urgente e ink para a entrega comum.
export function corDoRegistro(registro: Registro, agora: Date): string {
  if (registro.tipo === 'EVENTO') return corDaCategoria(registro.evento.categoria)
  return entregaUrgente(registro.entrega, agora) ? 'var(--ember)' : 'var(--ink)'
}
