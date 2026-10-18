import { Calendar, House, Settings, TrendingUp } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { CAMINHOS } from '../../lib/caminhos'

export interface ItemNavegacao {
  rotulo: string
  caminho: string
  Icone: LucideIcon
}

export const itensPrincipais: ItemNavegacao[] = [
  { rotulo: 'Today', caminho: CAMINHOS.inicio, Icone: House },
  { rotulo: 'Insights', caminho: CAMINHOS.insights, Icone: TrendingUp },
  { rotulo: 'Calendar', caminho: CAMINHOS.calendario, Icone: Calendar },
]

export const itemSettings: ItemNavegacao = {
  rotulo: 'Settings',
  caminho: CAMINHOS.ajustes,
  Icone: Settings,
}

export const itensNavegacao: ItemNavegacao[] = [...itensPrincipais, itemSettings]
