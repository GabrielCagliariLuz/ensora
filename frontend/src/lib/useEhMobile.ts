import { useSyncExternalStore } from 'react'

// Mesmo corte do AppShell: a partir de 768px a sidebar substitui a bottom nav.
const CONSULTA = '(max-width: 767px)'

function assinar(aoMudar: () => void): () => void {
  const lista = window.matchMedia(CONSULTA)
  lista.addEventListener('change', aoMudar)
  return () => lista.removeEventListener('change', aoMudar)
}

// A tela é de celular (abaixo de 768px)? Acompanha o redimensionamento.
export function useEhMobile(): boolean {
  return useSyncExternalStore(
    assinar,
    () => window.matchMedia(CONSULTA).matches,
    () => false,
  )
}
