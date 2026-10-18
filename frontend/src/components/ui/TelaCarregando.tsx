import { useEffect, useState } from 'react'
import AnelEnsora from './AnelEnsora'
import styles from './TelaCarregando.module.css'

// O backend no Render dorme sem uso e leva até ~1 min para acordar.
const ESPERA_AVISO_MS = 5000

export default function TelaCarregando() {
  const [demorando, setDemorando] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setDemorando(true), ESPERA_AVISO_MS)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div className={styles.tela}>
      <AnelEnsora className={styles.anel} />
      <span className={styles.status} role="status">
        Conectando…
      </span>
      <p className={styles.aviso}>
        {demorando && 'O servidor está acordando. Pode levar até um minuto.'}
      </p>
    </div>
  )
}
