import { SquarePen, Trash2 } from 'lucide-react'
import type { Habito } from '../../api/tipos'
import styles from './ListaHabitos.module.css'

interface ListaHabitosProps {
  habitos: Habito[]
  aoEditar: (habito: Habito) => void
  aoExcluir: (habito: Habito) => void
}

// Só apresentação: carregando, erro e vazio ficam com quem usa a lista.
export default function ListaHabitos({
  habitos,
  aoEditar,
  aoExcluir,
}: ListaHabitosProps) {
  return (
    <ul className={styles.lista}>
      {habitos.map((habito) => (
        <li key={habito.id} className={styles.item}>
          <span className={styles.nome}>{habito.nome}</span>
          <div className={styles.acoes}>
            <button
              className={styles.botao}
              type="button"
              aria-label={`Editar ${habito.nome}`}
              onClick={() => aoEditar(habito)}
            >
              <SquarePen size={18} strokeWidth={1.5} />
            </button>
            <button
              className={styles.botao}
              type="button"
              aria-label={`Excluir ${habito.nome}`}
              onClick={() => aoExcluir(habito)}
            >
              <Trash2 size={18} strokeWidth={1.5} />
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
