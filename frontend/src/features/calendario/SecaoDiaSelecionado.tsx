import type { Entrega } from '../../api/tipos'
import { rotuloDiaCurto } from '../../lib/datas'
import type { Registro } from './agenda'
import { idDoRegistro } from './mover'
import LinhaRegistro from './LinhaRegistro'
import styles from './SecaoMobile.module.css'

interface SecaoDiaSelecionadoProps {
  dia: string
  // Troca o rótulo do dia ("Sexta 9/10") por outro título.
  titulo?: string
  // Os registros do dia, em ordem cronológica (de registrosDoDia).
  registros: Registro[]
  agora: Date
  aoAbrir: (registro: Registro) => void
  aoAlternarEntrega: (entrega: Entrega) => void
}

function contagem(quantidade: number): string {
  return quantidade === 1 ? '1 registro' : `${quantidade} registros`
}

// Lista do dia escolhido no cartão do mês.
export default function SecaoDiaSelecionado({
  dia,
  titulo,
  registros,
  agora,
  aoAbrir,
  aoAlternarEntrega,
}: SecaoDiaSelecionadoProps) {
  return (
    <section className={styles.cartao} aria-live="polite">
      <header className={styles.cabecalho}>
        <h2 className={styles.titulo}>{titulo ?? rotuloDiaCurto(dia)}</h2>
        <span className={styles.rotulo}>{contagem(registros.length)}</span>
      </header>
      {registros.length === 0 ? (
        <p className={styles.vazio}>Nada neste dia.</p>
      ) : (
        <ul className={styles.lista}>
          {registros.map((registro) => (
            <li key={`${registro.tipo}-${idDoRegistro(registro)}`}>
              <LinhaRegistro
                registro={registro}
                agora={agora}
                aoAbrir={() => aoAbrir(registro)}
                aoAlternarEntrega={aoAlternarEntrega}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
