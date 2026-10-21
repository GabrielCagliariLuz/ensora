import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import CabecalhoPagina from '../../components/ui/CabecalhoPagina'
import { rotuloDoMes } from '../../lib/datas'
import styles from './CabecalhoCalendario.module.css'

interface CabecalhoCalendarioProps {
  // Mês visível ('AAAA-MM').
  mes: string
  aoAnterior: () => void
  aoProximo: () => void
  aoHoje: () => void
  aoNovoRegistro: () => void
}

export default function CabecalhoCalendario({
  mes,
  aoAnterior,
  aoProximo,
  aoHoje,
  aoNovoRegistro,
}: CabecalhoCalendarioProps) {
  return (
    <header className={styles.cabecalho}>
      <CabecalhoPagina titulo="Calendar" />
      <div className={styles.controles}>
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
        <button className={styles.hoje} type="button" onClick={aoHoje}>
          Hoje
        </button>
        <button className={styles.novo} type="button" onClick={aoNovoRegistro}>
          <Plus size={14} strokeWidth={1.5} />
          Novo Registro
        </button>
      </div>
    </header>
  )
}
