import { CircleAlert } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import styles from './BlocoAviso.module.css'

interface BlocoAvisoProps {
  // erro: algo falhou (ember, anunciado como alerta). aviso: informação neutra,
  // como estar sem conexão (anunciada como status).
  tom: 'erro' | 'aviso'
  icone?: LucideIcon
  children: ReactNode
}

export default function BlocoAviso({
  tom,
  icone: Icone = CircleAlert,
  children,
}: BlocoAvisoProps) {
  return (
    <div
      className={`${styles.bloco} ${tom === 'erro' ? styles.erro : styles.aviso}`}
      role={tom === 'erro' ? 'alert' : 'status'}
    >
      <Icone size={16} strokeWidth={1.5} />
      <span>{children}</span>
    </div>
  )
}
