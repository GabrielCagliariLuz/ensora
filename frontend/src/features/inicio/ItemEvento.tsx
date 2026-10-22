import { Link } from 'react-router'
import type { Evento } from '../../api/tipos'
import { calendarioNoDia } from '../../lib/caminhos'
import { diaDaSemana, diaDoMes } from '../../lib/datas'
import { corDaCategoria } from '../agenda/categorias'
import {
  estadoEvento,
  formatarHorarioEvento,
  textoDiasEvento,
} from './upcoming'
import styles from './ItemEvento.module.css'

interface ItemEventoProps {
  evento: Evento
  // Dia de hoje (AAAA-MM-DD), para o "hoje", "amanhã" e "N dias".
  hoje: string
  // Relógio do aparelho: "agora" e "encerrado" saem dele.
  agora: Date
}

export default function ItemEvento({ evento, hoje, agora }: ItemEventoProps) {
  const estado = estadoEvento(evento, agora)
  const texto =
    estado === 'normal'
      ? textoDiasEvento(evento.data, hoje)
      : estado === 'agora'
        ? 'agora'
        : 'encerrado'

  return (
    // Tocar no evento abre o Calendar no dia dele, com os detalhes abertos.
    <Link
      className={`${styles.item} ${styles[estado]}`}
      to={calendarioNoDia(evento.data, { tipo: 'EVENTO', id: evento.id })}
    >
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
        <span className={styles.dias}>{texto}</span>
        <span className={styles.horario}>
          {formatarHorarioEvento(evento.inicio, evento.termino)}
        </span>
      </div>
    </Link>
  )
}
