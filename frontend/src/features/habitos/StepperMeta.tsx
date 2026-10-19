import { Minus, Plus } from 'lucide-react'
import { limitarMeta, META_MAXIMA, META_MINIMA } from './regras'
import styles from './StepperMeta.module.css'

interface StepperMetaProps {
  // id do elemento de texto que rotula o grupo (o chamador desenha o rótulo).
  idRotulo: string
  valor: number
  aoMudar: (valor: number) => void
}

export default function StepperMeta({
  idRotulo,
  valor,
  aoMudar,
}: StepperMetaProps) {
  return (
    <div className={styles.stepper} role="group" aria-labelledby={idRotulo}>
      <button
        className={styles.botao}
        type="button"
        aria-label="Diminuir meta"
        disabled={valor <= META_MINIMA}
        onClick={() => aoMudar(limitarMeta(valor - 1))}
      >
        <Minus size={16} strokeWidth={1.5} />
      </button>
      <span className={styles.valor} aria-live="polite">
        {valor}x por semana
      </span>
      <button
        className={styles.botao}
        type="button"
        aria-label="Aumentar meta"
        disabled={valor >= META_MAXIMA}
        onClick={() => aoMudar(limitarMeta(valor + 1))}
      >
        <Plus size={16} strokeWidth={1.5} />
      </button>
    </div>
  )
}
