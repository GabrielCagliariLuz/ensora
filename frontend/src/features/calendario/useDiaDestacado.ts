import { useEffect, useState } from 'react'

const DURACAO_MS = 2400

// O dia que chegou pela URL fica destacado na grade por um instante, para o olho
// achar onde está o registro, e depois volta ao normal.
export function useDiaDestacado(dia: string | null): string | null {
  const [destacado, setDestacado] = useState(dia)

  useEffect(() => {
    if (dia === null) return
    const espera = window.setTimeout(() => setDestacado(null), DURACAO_MS)
    return () => window.clearTimeout(espera)
  }, [dia])

  return destacado
}
