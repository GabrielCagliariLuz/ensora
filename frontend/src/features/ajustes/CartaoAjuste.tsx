import type { ReactNode } from 'react'
import styles from './CartaoAjuste.module.css'

interface CartaoAjusteProps {
  titulo: string
  // Zona de perigo (exclusão): o título fica em vermelho.
  perigo?: boolean
  children: ReactNode
}

// Um bloco da tela de Configurações: título em mono e o conteúdo num cartão.
export default function CartaoAjuste({
  titulo,
  perigo = false,
  children,
}: CartaoAjusteProps) {
  return (
    <section className={perigo ? `${styles.cartao} ${styles.perigo}` : styles.cartao}>
      <h2 className={styles.titulo}>{titulo}</h2>
      {children}
    </section>
  )
}
