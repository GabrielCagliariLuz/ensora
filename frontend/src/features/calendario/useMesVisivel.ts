import { useState } from 'react'
import { mesDoDia, somarMeses } from './grade'

// O mês que o Calendar mostra ('AAAA-MM'). Abre no mês de hoje; as setas
// andam um mês e "Hoje" volta ao mês atual.
export function useMesVisivel(hoje: string) {
  const [mes, setMes] = useState(() => mesDoDia(hoje))

  return {
    mes,
    anterior: () => setMes((atual) => somarMeses(atual, -1)),
    proximo: () => setMes((atual) => somarMeses(atual, 1)),
    irParaHoje: () => setMes(mesDoDia(hoje)),
  }
}
