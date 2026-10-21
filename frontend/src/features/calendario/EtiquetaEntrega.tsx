import type { Entrega } from '../../api/tipos'
import CaixaMarcacao from '../../components/ui/CaixaMarcacao'
import { entregaUrgente } from './cores'
import Etiqueta from './Etiqueta'
import styles from './EtiquetaEntrega.module.css'

interface EtiquetaEntregaProps {
  entrega: Entrega
  // Relógio do aparelho: o prazo crítico sai dele.
  agora: Date
  // Concluir ou desfazer, conforme o estado atual da entrega.
  aoAlternar: () => void
  aoAbrir: (elemento: HTMLElement) => void
  arrastando: boolean
  aoComecarArrasto: () => void
  aoTerminarArrasto: () => void
}

// Caixa de seleção e título. Pendente e urgente (atrasada, hoje ou amanhã):
// vermelha. Pendente com tempo: preta, sobre o branco do cartão. Concluída: esmaecida e riscada.
export default function EtiquetaEntrega({
  entrega,
  agora,
  aoAlternar,
  aoAbrir,
  arrastando,
  aoComecarArrasto,
  aoTerminarArrasto,
}: EtiquetaEntregaProps) {
  const urgente = entregaUrgente(entrega, agora)

  return (
    <Etiqueta
      cor={urgente ? 'var(--ember)' : 'var(--ink)'}
      solida={!urgente}
      esmaecida={entrega.concluida}
      dica={entrega.titulo}
      aoAbrir={aoAbrir}
      arrastando={arrastando}
      aoComecarArrasto={aoComecarArrasto}
      aoTerminarArrasto={aoTerminarArrasto}
      prefixo={
        <label className={styles.marcador}>
          {/* A caixa é o próprio rótulo do input: clicar em qualquer ponto dela
              marca, e o cursor de mão vale na caixa inteira. */}
          <input
            className={styles.entrada}
            type="checkbox"
            checked={entrega.concluida}
            onChange={aoAlternar}
            aria-label={`Concluir ${entrega.titulo}`}
          />
          <CaixaMarcacao marcada={entrega.concluida} tamanho="pequeno" />
        </label>
      }
    >
      <span
        className={
          entrega.concluida
            ? `${styles.titulo} ${styles.concluida}`
            : styles.titulo
        }
      >
        {entrega.titulo}
      </span>
    </Etiqueta>
  )
}
