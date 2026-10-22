import { melhorEPiorDia, percentual } from './ritmo'
import type { MediaDoDia } from './ritmo'
import styles from './RitmoPorDiaDaSemana.module.css'

interface RitmoPorDiaDaSemanaProps {
  // Sete médias, de segunda a domingo (ritmoPorDiaDaSemana).
  medias: MediaDoDia[]
}

// O ritmo médio de cada dia da semana no mês: mostra se o mês pesa mais em
// algum dia. Embaixo, o dia mais forte e o mais fraco, quando há diferença.
export default function RitmoPorDiaDaSemana({
  medias,
}: RitmoPorDiaDaSemanaProps) {
  const extremos = melhorEPiorDia(medias)
  const semDados = medias.every((dia) => dia.media === undefined)

  return (
    <section className={styles.cartao} aria-label="Ritmo por dia da semana">
      <h3 className={styles.titulo}>Por dia da semana</h3>

      {semDados ? (
        <p className={styles.vazio}>Ainda sem dias com ritmo.</p>
      ) : (
        <>
          <ul className={styles.barras}>
            {medias.map((dia) => {
              const melhor = extremos?.melhor.chave === dia.chave
              const pior = extremos?.pior.chave === dia.chave
              return (
                <li
                  key={dia.chave}
                  className={
                    melhor
                      ? `${styles.coluna} ${styles.melhor}`
                      : pior
                        ? `${styles.coluna} ${styles.pior}`
                        : styles.coluna
                  }
                >
                  <span className={styles.percentual}>
                    {percentual(dia.media)}
                  </span>
                  <span
                    className={styles.trilha}
                    role="img"
                    aria-label={`${dia.nome}: ${percentual(dia.media)}`}
                  >
                    {dia.media !== undefined && (
                      <span
                        className={styles.preenchimento}
                        style={{
                          height: `${Math.min(Math.max(dia.media, 0), 100)}%`,
                        }}
                      />
                    )}
                  </span>
                  <span className={styles.inicial}>{dia.inicial}</span>
                </li>
              )
            })}
          </ul>

          {extremos && (
            <dl className={styles.extremos}>
              <div>
                <dt>Melhor dia</dt>
                <dd>
                  {extremos.melhor.nome} · {percentual(extremos.melhor.media)}
                </dd>
              </div>
              <div>
                <dt>Pior dia</dt>
                <dd className={styles.valorPior}>
                  {extremos.pior.nome} · {percentual(extremos.pior.media)}
                </dd>
              </div>
            </dl>
          )}
        </>
      )}
    </section>
  )
}
