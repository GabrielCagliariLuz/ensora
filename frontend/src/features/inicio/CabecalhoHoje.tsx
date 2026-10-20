import { rotuloDia } from '../../lib/datas'
import styles from './CabecalhoHoje.module.css'

interface CabecalhoHojeProps {
  // Dia exibido, em AAAA-MM-DD.
  data: string
}

// Mobile: "TODAY · 26 SET" numa linha. Desktop: TODAY como título e a data
// numa linha abaixo.
export default function CabecalhoHoje({ data }: CabecalhoHojeProps) {
  return (
    <header className={styles.cabecalho}>
      <h1 className={styles.titulo}>
        Today
        <span className={styles.separador} aria-hidden="true">
          ·
        </span>
        <span className={styles.data}>{rotuloDia(data)}</span>
      </h1>
    </header>
  )
}
