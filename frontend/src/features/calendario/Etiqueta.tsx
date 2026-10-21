import type { CSSProperties, ReactNode } from 'react'
import styles from './Etiqueta.module.css'

interface EtiquetaProps {
  // Cor da etiqueta (um token de cor): vira a faixa lateral, a borda e o fundo
  // tingidos e o texto.
  cor: string
  esmaecida?: boolean
  // Fundo branco em vez do tingido pela cor: para a etiqueta preta, que tingida
  // ficaria cinza.
  solida?: boolean
  // Texto completo, para o título cortado.
  dica: string
  // Abre os detalhes. Recebe o botão clicado, de onde o popover se ancora.
  aoAbrir: (elemento: HTMLElement) => void
  // Fica antes do botão, fora da área que abre os detalhes (a caixa da entrega).
  prefixo?: ReactNode
  // Arrastar para outro dia. Sem aoComecarArrasto a etiqueta não é arrastável.
  arrastando?: boolean
  aoComecarArrasto?: () => void
  aoTerminarArrasto?: () => void
  children: ReactNode
}

// Base das etiquetas de evento e de entrega dentro da célula do dia.
export default function Etiqueta({
  cor,
  esmaecida = false,
  solida = false,
  dica,
  aoAbrir,
  prefixo,
  arrastando = false,
  aoComecarArrasto,
  aoTerminarArrasto,
  children,
}: EtiquetaProps) {
  const classes = [
    styles.etiqueta,
    solida && styles.solida,
    esmaecida && styles.esmaecida,
    arrastando && styles.arrastando,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div
      className={classes}
      style={{ '--cor-etiqueta': cor } as CSSProperties}
      title={dica}
      draggable={aoComecarArrasto !== undefined}
      onDragStart={(evento) => {
        // O Firefox só inicia o arraste se houver dado.
        evento.dataTransfer.effectAllowed = 'move'
        evento.dataTransfer.setData('text/plain', dica)
        aoComecarArrasto?.()
      }}
      onDragEnd={aoTerminarArrasto}
    >
      {prefixo !== undefined && <span className={styles.prefixo}>{prefixo}</span>}
      <button
        className={styles.corpo}
        type="button"
        aria-haspopup="dialog"
        onClick={(evento) => aoAbrir(evento.currentTarget)}
      >
        {children}
      </button>
    </div>
  )
}
