import { Link } from 'react-router'
import { AlarmClock, Check, CircleAlert, Clock, Flag } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Entrega } from '../../api/tipos'
import IconeCheck from '../../components/ui/IconeCheck'
import { calendarioNoDia } from '../../lib/caminhos'
import { contagemPrazo, legendaPrazo } from './upcoming'
import type { EstadoPrazo } from './upcoming'
import styles from './ItemEntrega.module.css'

interface ItemEntregaProps {
  entrega: Entrega
  // Relógio do aparelho: "atrasada" e "N dias" saem dele.
  agora: Date
  // Concluir ou desfazer, conforme o estado atual da entrega.
  aoAlternar: () => void
}

// O ícone conta a situação antes de qualquer leitura: bandeira (sem pressa),
// relógio amarelo (de 24h a 48h), despertador (menos de 24h), alerta (atrasada)
// e check (concluída).
const ICONES: Record<EstadoPrazo, LucideIcon> = {
  normal: Flag,
  atencao: Clock,
  urgente: AlarmClock,
  atrasada: CircleAlert,
  concluida: Check,
}

// O que importa aqui é para quando é: o ícone, a data e a contagem dizem isso
// de relance. Concluir é secundário, uma caixa discreta no fim da linha.
export default function ItemEntrega({
  entrega,
  agora,
  aoAlternar,
}: ItemEntregaProps) {
  const contagem = contagemPrazo(entrega, agora)
  const Icone = ICONES[contagem.estado]

  return (
    <div className={`${styles.item} ${styles[contagem.estado]}`}>
      {/* Tocar na entrega abre o Calendar no dia dela, com os detalhes dela
          abertos. O link cobre a linha toda (menos a caixa de concluir). */}
      <Link
        className={styles.conteudo}
        to={calendarioNoDia(entrega.prazoData, {
          tipo: 'ENTREGA',
          id: entrega.id,
        })}
      >
        <span className={styles.icone} aria-hidden="true">
          <Icone size={20} strokeWidth={1.5} />
        </span>
        <div className={styles.texto}>
          <span className={styles.titulo}>{entrega.titulo}</span>
          <span className={styles.legenda}>{legendaPrazo(entrega)}</span>
        </div>
        <div className={styles.quando}>
          <span className={styles.contagem}>{contagem.texto}</span>
          {contagem.apoio && (
            <span className={styles.apoio}>{contagem.apoio}</span>
          )}
        </div>
      </Link>
      <button
        className={styles.marcar}
        type="button"
        onClick={aoAlternar}
        aria-pressed={entrega.concluida}
        aria-label={`Concluir ${entrega.titulo}`}
      >
        <span className={styles.circulo} aria-hidden="true">
          <IconeCheck size={12} />
        </span>
      </button>
    </div>
  )
}
