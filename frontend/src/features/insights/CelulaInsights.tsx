import { ROTULO_VARIANTE } from './estados'
import type { VarianteCelula } from './estados'
import styles from './CelulaInsights.module.css'

const CLASSE_DA_VARIANTE: Record<VarianteCelula, string> = {
  CONCLUIDO: styles.concluido,
  DESCANSO: styles.descanso,
  FUTURO: styles.futuro,
  NAO_REALIZADO: styles.naoRealizado,
  EM_ABERTO: styles.emAberto,
}

interface CelulaInsightsProps {
  variante: VarianteCelula
  // Para quem não vê: "Ler, 9 de setembro". Sem ele, a bolinha é só decoração
  // (a legenda).
  descricao?: string
}

// A bolinha de uma célula. A forma diz o estado, não só a cor: cheia
// (concluído), pontinho (descanso), anel fraco (futuro), anel rosado
// (não realizado) e anel tracejado (em aberto). O tamanho vem de
// --tamanho-celula, definido por quem usa (maior no celular).
export default function CelulaInsights({
  variante,
  descricao,
}: CelulaInsightsProps) {
  const classes = `${styles.celula} ${CLASSE_DA_VARIANTE[variante]}`

  if (descricao === undefined) {
    return <span className={classes} aria-hidden="true" />
  }

  const texto = `${descricao}: ${ROTULO_VARIANTE[variante].toLowerCase()}`
  return (
    <span className={classes} title={texto}>
      <span className={styles.apenasLeitor}>{texto}</span>
    </span>
  )
}
