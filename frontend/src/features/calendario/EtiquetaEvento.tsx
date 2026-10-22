import type { Evento } from '../../api/tipos'
import { corDaCategoria } from '../agenda/categorias'
import { horaCurta } from './agenda'
import Etiqueta from './Etiqueta'
import styles from './EtiquetaEvento.module.css'

interface EtiquetaEventoProps {
  evento: Evento
  aoAbrir: (elemento: HTMLElement) => void
  arrastando: boolean
  aoComecarArrasto: () => void
  aoTerminarArrasto: () => void
}

// Horário de início e título, na cor da categoria (neutra sem categoria).
export default function EtiquetaEvento({
  evento,
  aoAbrir,
  arrastando,
  aoComecarArrasto,
  aoTerminarArrasto,
}: EtiquetaEventoProps) {
  const hora = horaCurta(evento.inicio)

  return (
    <Etiqueta
      cor={corDaCategoria(evento.categoria)}
      dica={`${hora} ${evento.titulo}`}
      identificador={`EVENTO-${evento.id}`}
      aoAbrir={aoAbrir}
      arrastando={arrastando}
      aoComecarArrasto={aoComecarArrasto}
      aoTerminarArrasto={aoTerminarArrasto}
    >
      <span className={styles.hora}>{hora}</span>
      <span className={styles.titulo}>{evento.titulo}</span>
    </Etiqueta>
  )
}
