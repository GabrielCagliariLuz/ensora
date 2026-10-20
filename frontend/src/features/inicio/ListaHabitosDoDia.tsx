import type { HabitoNaHome } from '../../api/tipos'
import ItemHabito from './ItemHabito'
import styles from './ListaHabitosDoDia.module.css'

interface ListaHabitosDoDiaProps {
  habitos: HabitoNaHome[]
  aoAlternar: (habito: HabitoNaHome) => void
}

// Só apresentação: carregando, erro e vazio ficam com a página.
export default function ListaHabitosDoDia({
  habitos,
  aoAlternar,
}: ListaHabitosDoDiaProps) {
  return (
    <ul className={styles.lista}>
      {habitos.map((habito) => (
        <li key={habito.id}>
          <ItemHabito
            nome={habito.nome}
            feito={habito.feito}
            // feitos e meta só vêm no semanal; sem eles o cartão é de diário.
            progresso={
              habito.feitos !== undefined && habito.meta !== undefined
                ? { feitos: habito.feitos, meta: habito.meta }
                : undefined
            }
            aoAlternar={() => aoAlternar(habito)}
          />
        </li>
      ))}
    </ul>
  )
}
