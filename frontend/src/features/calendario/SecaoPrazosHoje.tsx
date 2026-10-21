import type { Entrega } from '../../api/tipos'
import { diaDaSemana, diaDoMes } from '../../lib/datas'
import type { Registro } from './agenda'
import LinhaRegistro from './LinhaRegistro'
import styles from './SecaoMobile.module.css'

interface SecaoPrazosHojeProps {
  entregas: Entrega[]
  hoje: string
  agora: Date
  aoAbrir: (registro: Registro) => void
  aoAlternarEntrega: (entrega: Entrega) => void
}

// Entregas com prazo hoje e as atrasadas ainda pendentes (de prazosDeHoje).
export default function SecaoPrazosHoje({
  entregas,
  hoje,
  agora,
  aoAbrir,
  aoAlternarEntrega,
}: SecaoPrazosHojeProps) {
  return (
    <section className={styles.cartao}>
      <header className={styles.cabecalho}>
        <h2 className={styles.titulo}>Prazos de Hoje</h2>
        <span className={styles.rotulo}>
          {diaDaSemana(hoje)} {diaDoMes(hoje)}
        </span>
      </header>
      {entregas.length === 0 ? (
        <p className={styles.vazio}>Nenhum prazo para hoje.</p>
      ) : (
        <ul className={styles.lista}>
          {entregas.map((entrega) => {
            const registro: Registro = { tipo: 'ENTREGA', entrega }
            return (
              <li key={entrega.id}>
                <LinhaRegistro
                  registro={registro}
                  agora={agora}
                  aoAbrir={() => aoAbrir(registro)}
                  aoAlternarEntrega={aoAlternarEntrega}
                />
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
