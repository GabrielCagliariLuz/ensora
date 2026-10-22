import { LIMITE_DESTAQUE } from './cartoes'
import { percentual } from './ritmo'
import styles from './AnelConsistencia.module.css'

// Raio 15 num quadro de 36: com o traço de 4, o anel ocupa o quadro todo.
const RAIO = 15
const CIRCUNFERENCIA = 2 * Math.PI * RAIO

interface AnelConsistenciaProps {
  // Percentual de 0 a 100; sem valor (nada a considerar), o anel fica vazio.
  valor?: number
  nome: string
}

// O anel de consistência de um hábito, com o percentual embaixo. Verde a partir
// de LIMITE_DESTAQUE, escuro abaixo.
export default function AnelConsistencia({
  valor,
  nome,
}: AnelConsistenciaProps) {
  const destaque = valor !== undefined && valor >= LIMITE_DESTAQUE
  const preenchido =
    valor === undefined
      ? 0
      : (Math.min(Math.max(valor, 0), 100) / 100) * CIRCUNFERENCIA

  return (
    <div className={destaque ? `${styles.anel} ${styles.verde}` : styles.anel}>
      <svg
        className={styles.svg}
        viewBox="0 0 36 36"
        role="img"
        aria-label={`${nome}: ${percentual(valor)} de consistência`}
      >
        <circle className={styles.trilha} cx="18" cy="18" r={RAIO} />
        <circle
          className={styles.progresso}
          cx="18"
          cy="18"
          r={RAIO}
          style={{ strokeDasharray: `${preenchido} ${CIRCUNFERENCIA}` }}
        />
      </svg>
      <span className={styles.valor} aria-hidden="true">
        {percentual(valor)}
      </span>
    </div>
  )
}
