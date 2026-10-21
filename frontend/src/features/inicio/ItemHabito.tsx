import CaixaMarcacao from '../../components/ui/CaixaMarcacao'
import { textoProgresso } from './regras'
import type { ProgressoSemanal } from './regras'
import styles from './ItemHabito.module.css'

interface ItemHabitoProps {
  nome: string
  feito: boolean
  // Só o hábito semanal tem progresso; sem ele o cartão é de hábito diário.
  progresso?: ProgressoSemanal
  // Sem esta função o cartão é só leitura: a caixa fica desabilitada.
  aoAlternar?: () => void
}

export default function ItemHabito({
  nome,
  feito,
  progresso,
  aoAlternar,
}: ItemHabitoProps) {
  const interativo = aoAlternar !== undefined

  return (
    <label
      className={
        interativo ? styles.item : `${styles.item} ${styles.somenteLeitura}`
      }
    >
      <span className={styles.texto}>
        <span className={feito ? `${styles.nome} ${styles.feito}` : styles.nome}>
          {nome}
        </span>
        {progresso && (
          <span className={styles.progresso}>{textoProgresso(progresso)}</span>
        )}
      </span>
      {/* O input nativo cobre o cartão inteiro, invisível: recebe clique e foco. */}
      <input
        className={styles.entrada}
        type="checkbox"
        checked={feito}
        disabled={!interativo}
        onChange={aoAlternar}
      />
      <CaixaMarcacao marcada={feito} />
    </label>
  )
}
