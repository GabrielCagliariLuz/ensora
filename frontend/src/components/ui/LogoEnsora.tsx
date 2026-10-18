import styles from './LogoEnsora.module.css'

interface LogoEnsoraProps {
  somenteAnel?: boolean
}

export default function LogoEnsora({ somenteAnel = false }: LogoEnsoraProps) {
  return (
    <span className={styles.logo}>
      <svg
        className={styles.anel}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="3.5"
        strokeLinecap="round"
        aria-hidden="true"
      >
        <circle
          cx="12"
          cy="12"
          r="9"
          strokeDasharray="46 10.55"
          transform="rotate(123 12 12)"
        />
      </svg>
      <span className={somenteAnel ? styles.textoOculto : styles.texto}>
        ENSORA
      </span>
    </span>
  )
}
