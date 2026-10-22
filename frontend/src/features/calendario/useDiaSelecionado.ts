import { useState } from 'react'
import { diaSelecionado } from './diaSelecionado'

// O dia que a visão mobile mostra na lista. Pode abrir já num dia (o da URL).
// Trocar de mês descarta a escolha: o dia vira o inicial do novo mês (hoje, ou
// o dia 1).
export function useDiaSelecionado(
  mes: string,
  hoje: string,
  diaInicial: string | null = null,
) {
  const [escolhido, escolher] = useState<string | null>(diaInicial)

  return { dia: diaSelecionado(escolhido, mes, hoje), escolher }
}
