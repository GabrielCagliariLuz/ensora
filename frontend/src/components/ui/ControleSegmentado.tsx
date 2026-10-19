import { useId } from 'react'
import styles from './ControleSegmentado.module.css'

interface OpcaoSegmentada<T extends string> {
  valor: T
  rotulo: string
}

interface ControleSegmentadoProps<T extends string> {
  // id do elemento de texto que rotula o grupo (o chamador desenha o rótulo).
  idRotulo: string
  opcoes: OpcaoSegmentada<T>[]
  valor: T
  aoMudar: (valor: T) => void
  desabilitado?: boolean
}

// Radios nativos com cara de abas: setas do teclado, foco e leitor de tela
// funcionam sem código extra. Serve a Diário/Semanal e, depois, a
// Evento/Entrega.
export default function ControleSegmentado<T extends string>({
  idRotulo,
  opcoes,
  valor,
  aoMudar,
  desabilitado = false,
}: ControleSegmentadoProps<T>) {
  const nome = useId()

  return (
    <div
      className={
        desabilitado ? `${styles.grupo} ${styles.desabilitado}` : styles.grupo
      }
      role="radiogroup"
      aria-labelledby={idRotulo}
    >
      {opcoes.map((opcao) => (
        <label key={opcao.valor} className={styles.opcao}>
          <input
            className={styles.radio}
            type="radio"
            name={nome}
            value={opcao.valor}
            checked={opcao.valor === valor}
            disabled={desabilitado}
            onChange={() => aoMudar(opcao.valor)}
          />
          <span className={styles.texto}>{opcao.rotulo}</span>
        </label>
      ))}
    </div>
  )
}
