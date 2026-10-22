import AnelConsistencia from './AnelConsistencia'
import { textoDaContagem, textoDoDelta } from './cartoes'
import type { CartaoDoHabito } from './cartoes'
import styles from './CartaoHabito.module.css'

interface CartaoHabitoProps {
  cartao: CartaoDoHabito
}

// O cartão de um hábito: o anel de consistência, o nome, a contagem que gerou o
// percentual e a diferença para o mês anterior.
export default function CartaoHabito({ cartao }: CartaoHabitoProps) {
  const { nome, excluido, consistencia, contagem, delta } = cartao

  return (
    <article className={styles.cartao}>
      <AnelConsistencia valor={consistencia} nome={nome} />
      <div className={styles.texto}>
        <h3
          className={excluido ? `${styles.nome} ${styles.excluido}` : styles.nome}
          title={excluido ? `${nome} (hábito excluído)` : nome}
        >
          {nome}
        </h3>
        <p className={styles.contagem}>{textoDaContagem(contagem)}</p>
        {delta !== null && (
          <p
            className={
              delta > 0
                ? `${styles.delta} ${styles.sobe}`
                : delta < 0
                  ? `${styles.delta} ${styles.desce}`
                  : styles.delta
            }
          >
            {textoDoDelta(delta)}
            <span className={styles.apenasLeitor}> em relação ao mês anterior</span>
          </p>
        )}
      </div>
    </article>
  )
}
