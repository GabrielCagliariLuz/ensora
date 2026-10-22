import { useCallback, useLayoutEffect, useRef } from 'react'

const DURACAO_MS = 360
const SUAVIZACAO = 'cubic-bezier(0.22, 0.8, 0.26, 1)'

// Anima a troca de lugar dos itens de uma lista quando a ordem muda (técnica
// FLIP): cada item que mudou de posição parte de onde estava e desliza até onde
// está. Quem desce passa por cima de quem sobe, com fundo próprio, para o
// cruzamento não embaralhar o texto.
//
// `ordem` é uma chave que muda quando a ordem muda (ex.: os ids unidos). O
// retorno é a função que liga cada item ao seu id: `ref={registrar(id)}`.
export function useAnimarOrdem(ordem: string) {
  const itens = useRef(new Map<string | number, HTMLElement>())
  const posicoes = useRef(new Map<string | number, number>())
  const ordemAnterior = useRef(ordem)

  const registrar = useCallback(
    (id: string | number) => (elemento: HTMLElement | null) => {
      if (elemento) itens.current.set(id, elemento)
      else itens.current.delete(id)
    },
    [],
  )

  // Sem lista de dependências de propósito: as posições são lidas a cada
  // renderização, para a comparação nunca partir de um layout velho (ex.: depois
  // de redimensionar a janela). offsetTop ignora transformações em andamento.
  useLayoutEffect(() => {
    const reduzir = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const mudou = ordemAnterior.current !== ordem
    ordemAnterior.current = ordem

    const novas = new Map<string | number, number>()
    itens.current.forEach((elemento, id) => {
      const topo = elemento.offsetTop
      novas.set(id, topo)

      const antes = posicoes.current.get(id)
      if (!mudou || reduzir || antes === undefined || antes === topo) return

      const desce = topo > antes
      if (desce) {
        elemento.style.position = 'relative'
        elemento.style.zIndex = '1'
        elemento.style.background = 'var(--card)'
      }
      const animacao = elemento.animate(
        [
          { transform: `translateY(${antes - topo}px)` },
          { transform: 'translateY(0)' },
        ],
        { duration: DURACAO_MS, easing: SUAVIZACAO },
      )
      animacao.onfinish = animacao.oncancel = () => {
        elemento.style.position = ''
        elemento.style.zIndex = ''
        elemento.style.background = ''
      }
    })
    posicoes.current = novas
  })

  return registrar
}
