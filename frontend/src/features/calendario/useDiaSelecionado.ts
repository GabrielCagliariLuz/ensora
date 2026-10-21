import { useState } from 'react'
import { diaSelecionado } from './diaSelecionado'

// O dia que a visão mobile mostra na lista. Trocar de mês descarta a escolha:
// o dia vira o inicial do novo mês (hoje, ou o dia 1).
export function useDiaSelecionado(mes: string, hoje: string) {
  const [escolhido, escolher] = useState<string | null>(null)

  return { dia: diaSelecionado(escolhido, mes, hoje), escolher }
}
