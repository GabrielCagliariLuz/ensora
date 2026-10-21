import type { Entrega } from '../../api/tipos'
import { rotuloPrazo, situacaoPrazo } from './upcoming'
import styles from './ItemEntrega.module.css'

interface ItemEntregaProps {
  entrega: Entrega
  // Relógio do aparelho: "atrasada" e "vence em N dias" saem dele.
  agora: Date
  // Concluir ou desfazer, conforme o estado atual da entrega.
  aoAlternar: () => void
}

export default function ItemEntrega({
  entrega,
  agora,
  aoAlternar,
}: ItemEntregaProps) {
  const situacao = situacaoPrazo(entrega, agora)

  return (
    <div className={styles.item}>
      <div className={styles.texto}>
        <span
          className={
            entrega.concluida
              ? `${styles.titulo} ${styles.concluida}`
              : styles.titulo
          }
        >
          {entrega.titulo}
        </span>
        <span className={styles.prazo}>
          {rotuloPrazo(entrega)} ·{' '}
          <span className={situacao.urgente ? styles.urgente : undefined}>
            {situacao.texto}
          </span>
        </span>
      </div>
      <button
        className={
          entrega.concluida
            ? `${styles.botao} ${styles.desfazer}`
            : styles.botao
        }
        type="button"
        onClick={aoAlternar}
        aria-label={
          entrega.concluida
            ? `Desfazer conclusão de ${entrega.titulo}`
            : `Concluir ${entrega.titulo}`
        }
      >
        {entrega.concluida ? 'Desfazer' : 'Concluir'}
      </button>
    </div>
  )
}
