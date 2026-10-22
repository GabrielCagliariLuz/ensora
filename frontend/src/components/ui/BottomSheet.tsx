import { useId } from 'react'
import type { ReactNode } from 'react'
import { X } from 'lucide-react'
import CaixaDialogo from './CaixaDialogo'
import { useArrastarParaFechar } from './useArrastarParaFechar'
import styles from './BottomSheet.module.css'

interface BottomSheetProps {
  aberto: boolean
  aoFechar: () => void
  titulo: string
  children: ReactNode
}

// No mobile sobe colado embaixo e fecha arrastando a alça para baixo; no
// desktop vira um cartão centralizado.
// Os filhos só existem enquanto aberto, então um formulário dentro começa
// limpo a cada abertura.
export default function BottomSheet({
  aberto,
  aoFechar,
  titulo,
  children,
}: BottomSheetProps) {
  const idTitulo = useId()
  const arraste = useArrastarParaFechar(aoFechar)

  return (
    <CaixaDialogo
      aberto={aberto}
      aoFechar={aoFechar}
      idTitulo={idTitulo}
      className={styles.sheet}
    >
      {/* Alça e cabeçalho formam a zona de arraste; o corpo não, para a
          rolagem e os campos continuarem normais. */}
      <div className={styles.zonaDeArraste} {...arraste}>
        <span className={styles.alca} aria-hidden="true" />
        <header className={styles.cabecalho}>
          <h2 id={idTitulo} className={styles.titulo}>
            {titulo}
          </h2>
          <button
            className={styles.fechar}
            type="button"
            onClick={aoFechar}
            aria-label="Fechar"
          >
            <X size={16} strokeWidth={1.5} />
          </button>
        </header>
      </div>
      <div className={styles.corpo}>{children}</div>
    </CaixaDialogo>
  )
}
