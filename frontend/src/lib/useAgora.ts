import { useEffect, useState } from 'react'

// O instante atual, atualizado a cada minuto e quando a aba volta a ficar
// visível ou ganha foco. Serve a textos que dependem da hora, como "atrasada":
// sem isto, uma entrega só viraria atrasada depois de outra renderização.
export function useAgora(intervaloEmMs = 60_000): Date {
  const [agora, setAgora] = useState(() => new Date())

  useEffect(() => {
    const atualizar = () => setAgora(new Date())
    const timer = setInterval(atualizar, intervaloEmMs)

    window.addEventListener('focus', atualizar)
    document.addEventListener('visibilitychange', atualizar)
    return () => {
      clearInterval(timer)
      window.removeEventListener('focus', atualizar)
      document.removeEventListener('visibilitychange', atualizar)
    }
  }, [intervaloEmMs])

  return agora
}
