import { useState } from 'react'
import { somarDias } from '../../lib/datas'
import { segundaDaSemana } from './semana'

// A semana que o Insights mobile mostra, pela sua segunda-feira (AAAA-MM-DD).
// Abre na semana de hoje; as setas andam de 7 em 7 dias.
export function useSemanaVisivel(hoje: string) {
  const [segunda, setSegunda] = useState(() => segundaDaSemana(hoje))

  return {
    segunda,
    anterior: () => setSegunda((atual) => somarDias(atual, -7)),
    proxima: () => setSegunda((atual) => somarDias(atual, 7)),
    irParaHoje: () => setSegunda(segundaDaSemana(hoje)),
  }
}
