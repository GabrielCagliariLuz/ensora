import styles from './CabecalhoPagina.module.css'

interface CabecalhoPaginaProps {
  titulo: string
  legenda?: string
}

export default function CabecalhoPagina({
  titulo,
  legenda,
}: CabecalhoPaginaProps) {
  return (
    <header className={styles.cabecalho}>
      <h1 className={styles.titulo}>{titulo}</h1>
      {legenda && <p className={styles.legenda}>{legenda}</p>}
    </header>
  )
}
