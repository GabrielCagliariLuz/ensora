import { useEffect, useState } from 'react'
import { hojeISO } from './datas'

// O dia de hoje (AAAA-MM-DD), recalculado quando a aba volta a ficar visível
// ou ganha foco. Sem isso, uma Home deixada aberta passaria da meia-noite
// pedindo o dia anterior. Se o dia não mudou, o estado é o mesmo e não há
// nova renderização.
export function useHoje(): string {
  const [hoje, setHoje] = useState(hojeISO)

  useEffect(() => {
    const atualizar = () => setHoje(hojeISO())

    window.addEventListener('focus', atualizar)
    document.addEventListener('visibilitychange', atualizar)
    return () => {
      window.removeEventListener('focus', atualizar)
      document.removeEventListener('visibilitychange', atualizar)
    }
  }, [])

  return hoje
}
