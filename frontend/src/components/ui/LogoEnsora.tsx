import AnelEnsora from './AnelEnsora'
import styles from './LogoEnsora.module.css'

interface LogoEnsoraProps {
  somenteAnel?: boolean
}

export default function LogoEnsora({ somenteAnel = false }: LogoEnsoraProps) {
  return (
    <span className={styles.logo}>
      <AnelEnsora className={styles.anel} />
      <span className={somenteAnel ? styles.textoOculto : styles.texto}>
        ENSORA
      </span>
    </span>
  )
}
