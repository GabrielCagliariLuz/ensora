import { useSyncExternalStore } from 'react'

function assinar(aoMudar: () => void): () => void {
  window.addEventListener('online', aoMudar)
  window.addEventListener('offline', aoMudar)
  return () => {
    window.removeEventListener('online', aoMudar)
    window.removeEventListener('offline', aoMudar)
  }
}

// navigator.onLine só garante o "sem conexão": online não prova que a API
// responde, então a falha de rede (ErroApi status 0) continua tratada à parte.
export function useOnline(): boolean {
  return useSyncExternalStore(
    assinar,
    () => navigator.onLine,
    () => true,
  )
}
