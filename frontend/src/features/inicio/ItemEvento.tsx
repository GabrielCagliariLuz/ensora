import type { Evento } from '../../api/tipos'
import { diaDaSemana, diaDoMes } from '../../lib/datas'
import { corDaCategoria } from '../agenda/categorias'
import { formatarHorarioEvento, textoDiasEvento } from './upcoming'
import styles from './ItemEvento.module.css'

interface ItemEventoProps {
  evento: Evento
  // Dia de hoje (AAAA-MM-DD), para o "hoje", "amanhã" e "N dias".
  hoje: string
}

export default function ItemEvento({ evento, hoje }: ItemEventoProps) {
  return (
    <div className={styles.item}>
      <div className={styles.selo}>
        {/* Ponto na cor da categoria; cinza neutro quando não há. */}
        <span
          className={styles.ponto}
          style={{ background: corDaCategoria(evento.categoria) }}
          aria-hidden="true"
        />
        <span className={styles.dia}>{diaDoMes(evento.data)}</span>
        <span className={styles.semana}>{diaDaSemana(evento.data)}</span>
      </div>
      <span className={styles.titulo}>{evento.titulo}</span>
      <div className={styles.quando}>
        <span className={styles.dias}>{textoDiasEvento(evento.data, hoje)}</span>
        <span className={styles.horario}>
          {formatarHorarioEvento(evento.inicio, evento.termino)}
        </span>
      </div>
    </div>
  )
}
