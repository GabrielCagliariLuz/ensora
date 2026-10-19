import { useId } from 'react'
import type { ReactNode } from 'react'
import { X } from 'lucide-react'
import CaixaDialogo from './CaixaDialogo'
import styles from './BottomSheet.module.css'

interface BottomSheetProps {
  aberto: boolean
  aoFechar: () => void
  titulo: string
  children: ReactNode
}

// No mobile sobe colado embaixo; no desktop vira um cartão centralizado.
// Os filhos só existem enquanto aberto, então um formulário dentro começa
// limpo a cada abertura.
export default function BottomSheet({
  aberto,
  aoFechar,
  titulo,
  children,
}: BottomSheetProps) {
  const idTitulo = useId()

  return (
    <CaixaDialogo
      aberto={aberto}
      aoFechar={aoFechar}
      idTitulo={idTitulo}
      className={styles.sheet}
    >
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
      <div className={styles.corpo}>{children}</div>
    </CaixaDialogo>
  )
}
