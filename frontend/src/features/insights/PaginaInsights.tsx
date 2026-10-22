import CabecalhoPagina from '../../components/ui/CabecalhoPagina'
import { useEhMobile } from '../../lib/useEhMobile'
import { useHoje } from '../../lib/useHoje'
import ConsistenciaMobile from './ConsistenciaMobile'
import InsightsDesktop from './InsightsDesktop'
import InsightsMobile from './InsightsMobile'
import styles from './PaginaInsights.module.css'

export default function PaginaInsights() {
  const hoje = useHoje()
  const ehMobile = useEhMobile()

  return (
    <div className={styles.pagina}>
      <CabecalhoPagina titulo="Insights" />

      {ehMobile ? (
        <>
          <section>
            <h2 className={styles.secao}>Ritmo da semana</h2>
            <InsightsMobile hoje={hoje} />
          </section>
          <ConsistenciaMobile hoje={hoje} />
        </>
      ) : (
        <InsightsDesktop hoje={hoje} />
      )}
    </div>
  )
}
