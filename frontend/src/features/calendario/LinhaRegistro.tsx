import type { CSSProperties } from 'react'
import type { Entrega } from '../../api/tipos'
import CaixaMarcacao from '../../components/ui/CaixaMarcacao'
import { horaCurta } from './agenda'
import type { Registro } from './agenda'
import { corDoRegistro, entregaUrgente } from './cores'
import styles from './LinhaRegistro.module.css'

interface LinhaRegistroProps {
  registro: Registro
  agora: Date
  // Abre os detalhes (toque no corpo da linha).
  aoAbrir: () => void
  // Concluir ou desfazer, pela caixa da entrega.
  aoAlternarEntrega: (entrega: Entrega) => void
}

// Linha da lista do mobile: tingida pela cor do registro, título à esquerda e
// horário à direita. A caixa da entrega conclui; o resto da linha abre os
// detalhes.
export default function LinhaRegistro({
  registro,
  agora,
  aoAbrir,
  aoAlternarEntrega,
}: LinhaRegistroProps) {
  const ehEntrega = registro.tipo === 'ENTREGA'
  const titulo = ehEntrega ? registro.entrega.titulo : registro.evento.titulo
  const horario = ehEntrega
    ? registro.entrega.prazoHorario
    : registro.evento.inicio
  const concluida = ehEntrega && registro.entrega.concluida
  // A entrega comum é preta sobre branco: tingida, ficaria cinza.
  const solida = ehEntrega && !entregaUrgente(registro.entrega, agora)

  const classes = [
    styles.linha,
    solida && styles.solida,
    concluida && styles.esmaecida,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div
      className={classes}
      style={{ '--cor-linha': corDoRegistro(registro, agora) } as CSSProperties}
    >
      {ehEntrega && (
        <label className={styles.marcador}>
          <input
            className={styles.entrada}
            type="checkbox"
            checked={registro.entrega.concluida}
            onChange={() => aoAlternarEntrega(registro.entrega)}
            aria-label={`Concluir ${registro.entrega.titulo}`}
          />
          <CaixaMarcacao marcada={registro.entrega.concluida} />
        </label>
      )}
      <button
        className={styles.corpo}
        type="button"
        aria-haspopup="dialog"
        onClick={aoAbrir}
      >
        <span
          className={
            concluida ? `${styles.titulo} ${styles.concluida}` : styles.titulo
          }
        >
          {titulo}
        </span>
        {horario && <span className={styles.horario}>{horaCurta(horario)}</span>}
      </button>
    </div>
  )
}
