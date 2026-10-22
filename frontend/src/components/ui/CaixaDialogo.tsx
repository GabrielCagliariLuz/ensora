import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { useVisualViewport } from '../../lib/useVisualViewport'
import styles from './CaixaDialogo.module.css'

// Base de BottomSheet e ModalConfirmacao: um <dialog> nativo aberto com
// showModal(), que já entrega Esc, foco preso, fundo inerte e camada acima de
// tudo. Vários podem abrir empilhados (o Esc fecha só o de cima).

// Contador, porque um modal pode abrir por cima de um sheet e o scroll da
// página só volta quando o último fecha.
let camadasAbertas = 0

function travarScroll(): void {
  if (camadasAbertas++ === 0) document.body.style.overflow = 'hidden'
}

function destravarScroll(): void {
  if (--camadasAbertas === 0) document.body.style.overflow = ''
}

interface CaixaDialogoProps {
  aberto: boolean
  aoFechar: () => void
  idTitulo: string
  className?: string
  children: ReactNode
}

export default function CaixaDialogo({
  aberto,
  aoFechar,
  idTitulo,
  className,
  children,
}: CaixaDialogoProps) {
  // Só enquanto aberto: a sheet acompanha o teclado do celular.
  useVisualViewport(aberto)
  const dialogo = useRef<HTMLDialogElement>(null)
  const pressionouNoFundo = useRef(false)

  useEffect(() => {
    const elemento = dialogo.current
    if (!elemento || !aberto) return

    elemento.showModal()
    travarScroll()
    return () => {
      // close() devolve o foco ao elemento que abriu o diálogo.
      elemento.close()
      destravarScroll()
    }
  }, [aberto])

  return (
    <dialog
      ref={dialogo}
      className={className ? `${styles.dialogo} ${className}` : styles.dialogo}
      aria-labelledby={idTitulo}
      // Componente controlado: o Esc só pede para fechar, e quem fecha é a prop.
      onCancel={(evento) => {
        evento.preventDefault()
        aoFechar()
      }}
      // Clique no fundo (::backdrop) tem o próprio <dialog> como alvo. O
      // mousedown evita fechar quando se arrasta uma seleção de texto de
      // dentro para fora.
      onMouseDown={(evento) => {
        pressionouNoFundo.current = evento.target === evento.currentTarget
      }}
      onClick={(evento) => {
        if (pressionouNoFundo.current && evento.target === evento.currentTarget) {
          aoFechar()
        }
      }}
    >
      {aberto ? children : null}
    </dialog>
  )
}
