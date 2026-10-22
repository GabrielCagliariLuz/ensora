import { rotuloDoMes } from '../../lib/datas'
import { percentual, rotuloDoIntervalo } from './ritmo'
import type { BarraSemanal } from './ritmo'
import styles from './RitmoDoMes.module.css'

interface RitmoDoMesProps {
  mes: string
  // Ritmo médio do mês (de 0 a 100, uma casa decimal); sem valor, um traço.
  ritmoMedio?: number
  barras: BarraSemanal[]
}

// O ritmo médio do mês em número grande e, ao lado, as quatro barras semanais
// (S1 a S4): cada uma com o percentual em cima e o intervalo de dias embaixo. A
// semana de hoje fica em destaque, e a que ainda não tem valor, vazia e tracejada.
export default function RitmoDoMes({
  mes,
  ritmoMedio,
  barras,
}: RitmoDoMesProps) {
  const nomeDoMes = rotuloDoMes(mes).split(' ')[0].toLowerCase()
  const inteiro = ritmoMedio === undefined ? '–' : String(Math.round(ritmoMedio))

  return (
    <section className={styles.cartao} aria-label="Ritmo médio do mês">
      <div className={styles.media}>
        <p className={styles.numero}>
          {inteiro}
          {ritmoMedio !== undefined && <span className={styles.simbolo}>%</span>}
        </p>
        <p className={styles.legenda}>ritmo médio em {nomeDoMes}</p>
      </div>

      <ul className={styles.barras}>
        {barras.map((barra) => (
          <li
            key={barra.rotulo}
            className={
              barra.atual ? `${styles.coluna} ${styles.atual}` : styles.coluna
            }
          >
            <span className={styles.percentual}>{percentual(barra.ritmo)}</span>
            <span
              className={
                barra.ritmo === undefined
                  ? `${styles.trilha} ${styles.vazia}`
                  : styles.trilha
              }
              role="img"
              aria-label={`${barra.rotulo}: ${percentual(barra.ritmo)}`}
            >
              {barra.ritmo !== undefined && (
                <span
                  className={styles.preenchimento}
                  style={{ height: `${Math.min(Math.max(barra.ritmo, 0), 100)}%` }}
                />
              )}
            </span>
            <span className={styles.rotulo}>{barra.rotulo}</span>
            <span className={styles.intervalo}>
              {rotuloDoIntervalo(barra.de, barra.ate)}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
