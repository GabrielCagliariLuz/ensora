import { useRef } from 'react'
import type { PointerEvent as EventoDePonteiro } from 'react'

// Quanto precisa descer para fechar, ou o quão rápido (px por ms) o dedo precisa
// soltar. Um arrasto bem curto nunca fecha: evita fechar por um toque trêmulo.
const DESLOCAMENTO_PARA_FECHAR = 100
const VELOCIDADE_PARA_FECHAR = 0.5
const DESLOCAMENTO_MINIMO = 24
const DURACAO_MS = 200

// Fechou? Passou da distância, ou soltou com pressa (e já tinha descido um pouco).
export function deveFechar(deslocamento: number, velocidade: number): boolean {
  return (
    deslocamento >= DESLOCAMENTO_PARA_FECHAR ||
    (deslocamento >= DESLOCAMENTO_MINIMO && velocidade >= VELOCIDADE_PARA_FECHAR)
  )
}

const ehCelular = () => window.matchMedia('(max-width: 767px)').matches
const reduzMovimento = () =>
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

interface Arrasto {
  y: number
  instante: number
  deslocamento: number
}

// Arrastar a alça e o cabeçalho para baixo fecha a sheet. Devolve os eventos
// para espalhar na zona de arraste. O botão de fechar não inicia o gesto, e no
// desktop (a partir de 768px) nada acontece.
export function useArrastarParaFechar(aoFechar: () => void) {
  const arrasto = useRef<Arrasto | null>(null)

  const dialogoDe = (evento: EventoDePonteiro<HTMLElement>) =>
    evento.currentTarget.closest('dialog')

  function aoPressionar(evento: EventoDePonteiro<HTMLElement>) {
    if (!ehCelular()) return
    if ((evento.target as HTMLElement).closest('button')) return
    const dialogo = dialogoDe(evento)
    if (!dialogo) return

    evento.currentTarget.setPointerCapture(evento.pointerId)
    arrasto.current = {
      y: evento.clientY,
      instante: evento.timeStamp,
      deslocamento: 0,
    }
    dialogo.style.transition = 'none'
  }

  function aoMover(evento: EventoDePonteiro<HTMLElement>) {
    const dialogo = dialogoDe(evento)
    const atual = arrasto.current
    if (!atual || !dialogo) return

    // Só para baixo: a sheet não passa do ponto onde está.
    atual.deslocamento = Math.max(0, evento.clientY - atual.y)
    dialogo.style.transform = `translateY(${atual.deslocamento}px)`
  }

  function limpar(dialogo: HTMLElement) {
    dialogo.style.transition = ''
    dialogo.style.transform = ''
  }

  function aoSoltar(evento: EventoDePonteiro<HTMLElement>) {
    const dialogo = dialogoDe(evento)
    const atual = arrasto.current
    arrasto.current = null
    if (!atual || !dialogo) return

    const duracao = Math.max(1, evento.timeStamp - atual.instante)
    const fechar = deveFechar(atual.deslocamento, atual.deslocamento / duracao)

    if (!fechar) {
      // Volta suavemente para onde estava.
      dialogo.style.transition = reduzMovimento() ? '' : `transform ${DURACAO_MS}ms ease`
      dialogo.style.transform = ''
      return
    }

    if (reduzMovimento()) {
      limpar(dialogo)
      aoFechar()
      return
    }
    // Desce até sumir e só então fecha; o estilo é limpo para a próxima abertura.
    dialogo.style.transition = `transform ${DURACAO_MS}ms ease`
    dialogo.style.transform = 'translateY(100%)'
    window.setTimeout(() => {
      aoFechar()
      limpar(dialogo)
    }, DURACAO_MS)
  }

  function aoCancelar(evento: EventoDePonteiro<HTMLElement>) {
    const dialogo = dialogoDe(evento)
    arrasto.current = null
    if (dialogo) limpar(dialogo)
  }

  return {
    onPointerDown: aoPressionar,
    onPointerMove: aoMover,
    onPointerUp: aoSoltar,
    onPointerCancel: aoCancelar,
  }
}
