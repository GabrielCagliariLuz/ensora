import { useState, useSyncExternalStore } from 'react'

const CHAVE = 'ensora:sidebar-colapsada'
const CONSULTA_DESKTOP = '(min-width: 1024px)'

function lerPreferencia(): boolean | null {
  try {
    const valor = localStorage.getItem(CHAVE)
    return valor === null ? null : valor === 'true'
  } catch {
    return null
  }
}

function salvarPreferencia(colapsada: boolean) {
  try {
    localStorage.setItem(CHAVE, String(colapsada))
  } catch {
    // Sem armazenamento (modo privado, bloqueio): a preferência vale só na sessão.
  }
}

function assinarLargura(aoMudar: () => void) {
  const consulta = window.matchMedia(CONSULTA_DESKTOP)
  consulta.addEventListener('change', aoMudar)
  return () => consulta.removeEventListener('change', aoMudar)
}

function larguraDesktop() {
  return window.matchMedia(CONSULTA_DESKTOP).matches
}

// Sem escolha do usuário, a Sidebar nasce colapsada abaixo de 1024px
// (faixa de tablet) e expandida a partir daí. Depois do primeiro clique no
// botão, a escolha do usuário vale em qualquer largura.
export function useSidebarColapsada() {
  const [preferencia, setPreferencia] = useState(lerPreferencia)
  const desktop = useSyncExternalStore(assinarLargura, larguraDesktop)
  const colapsada = preferencia ?? !desktop

  function alternar() {
    const proxima = !colapsada
    setPreferencia(proxima)
    salvarPreferencia(proxima)
  }

  return [colapsada, alternar] as const
}
