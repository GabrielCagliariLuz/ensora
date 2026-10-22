import CelulaInsights from './CelulaInsights'
import { ROTULO_VARIANTE } from './estados'
import type { VarianteCelula } from './estados'
import styles from './LegendaInsights.module.css'

const FORMAS: VarianteCelula[] = [
  'CONCLUIDO',
  'DESCANSO',
  'FUTURO',
  'NAO_REALIZADO',
  'EM_ABERTO',
]

// As cinco formas da grade, cada uma com o seu nome. Serve ao desktop e ao
// mobile.
export default function LegendaInsights() {
  return (
    <ul className={styles.estados}>
      {FORMAS.map((variante) => (
        <li key={variante} className={styles.item}>
          <CelulaInsights variante={variante} />
          {ROTULO_VARIANTE[variante]}
        </li>
      ))}
    </ul>
  )
}
