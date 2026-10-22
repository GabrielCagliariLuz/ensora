import { ChevronLeft, ChevronRight } from 'lucide-react'
import { rotuloDoMes } from '../../lib/datas'
import styles from './SeletorDeMes.module.css'

interface SeletorDeMesProps {
  // Mês visível ('AAAA-MM').
  mes: string
  aoAnterior: () => void
  aoProximo: () => void
}

// ‹ Setembro 2026 ›: troca o mês do bloco de consistência no mobile, onde a
// grade é semanal e não tem navegação de mês.
export default function SeletorDeMes({
  mes,
  aoAnterior,
  aoProximo,
}: SeletorDeMesProps) {
  return (
    <div className={styles.seletor}>
      <button
        className={styles.seta}
        type="button"
        onClick={aoAnterior}
        aria-label="Mês anterior"
      >
        <ChevronLeft size={16} strokeWidth={1.5} />
      </button>
      <span className={styles.mes} aria-live="polite">
        {rotuloDoMes(mes)}
      </span>
      <button
        className={styles.seta}
        type="button"
        onClick={aoProximo}
        aria-label="Próximo mês"
      >
        <ChevronRight size={16} strokeWidth={1.5} />
      </button>
    </div>
  )
}
