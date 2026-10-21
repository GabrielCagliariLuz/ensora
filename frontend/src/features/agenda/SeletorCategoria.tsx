import type { CategoriaEvento } from '../../api/tipos'
import { CATEGORIAS } from './categorias'
import styles from './SeletorCategoria.module.css'

interface SeletorCategoriaProps {
  // id do elemento de texto que rotula o grupo (o chamador desenha o rótulo).
  idRotulo: string
  valor: CategoriaEvento | null
  aoMudar: (valor: CategoriaEvento | null) => void
}

// A categoria é opcional: clicar na escolhida de novo a limpa.
export default function SeletorCategoria({
  idRotulo,
  valor,
  aoMudar,
}: SeletorCategoriaProps) {
  return (
    <div className={styles.grupo} role="group" aria-labelledby={idRotulo}>
      {CATEGORIAS.map((categoria) => (
        <button
          key={categoria.valor}
          className={styles.chip}
          type="button"
          aria-pressed={valor === categoria.valor}
          onClick={() => aoMudar(valor === categoria.valor ? null : categoria.valor)}
        >
          <span
            className={styles.ponto}
            style={{ background: categoria.cor }}
            aria-hidden="true"
          />
          {categoria.rotulo}
        </button>
      ))}
    </div>
  )
}
