import { useEffect } from 'react'

// Quanto do celular o teclado está tomando: a janela inteira menos a parte
// visível, descontando o quanto a página foi empurrada para cima. Nunca negativo.
export function espacoDoTeclado(
  alturaDaJanela: number,
  alturaVisivel: number,
  deslocamentoNoTopo: number,
): number {
  return Math.max(0, alturaDaJanela - alturaVisivel - deslocamentoNoTopo)
}

// Enquanto ativo, o teclado do celular encolhe a área visível, mas não o layout.
// O hook escreve duas variáveis no <html> (--vv-altura e --teclado) para a sheet
// se posicionar acima do teclado. Sem visualViewport, não faz nada.
export function useVisualViewport(ativo: boolean): void {
  useEffect(() => {
    const vv = window.visualViewport
    if (!ativo || !vv) return

    const raiz = document.documentElement
    let temporizador: number | undefined

    const atualizar = () => {
      raiz.style.setProperty('--vv-altura', `${vv.height}px`)
      raiz.style.setProperty(
        '--teclado',
        `${espacoDoTeclado(window.innerHeight, vv.height, vv.offsetTop)}px`,
      )
    }

    // Ao fechar o teclado (✓), o iOS deixa a página deslocada: volta ao topo.
    const aoSairDoCampo = () => {
      window.clearTimeout(temporizador)
      temporizador = window.setTimeout(() => {
        window.scrollTo(0, 0)
        atualizar()
      }, 100)
    }

    atualizar()
    vv.addEventListener('resize', atualizar)
    vv.addEventListener('scroll', atualizar)
    document.addEventListener('focusout', aoSairDoCampo)
    return () => {
      window.clearTimeout(temporizador)
      vv.removeEventListener('resize', atualizar)
      vv.removeEventListener('scroll', atualizar)
      document.removeEventListener('focusout', aoSairDoCampo)
      raiz.style.removeProperty('--vv-altura')
      raiz.style.removeProperty('--teclado')
    }
  }, [ativo])
}
