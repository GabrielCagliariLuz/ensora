import styles from './AcoesHabitos.module.css'

interface AcoesHabitosProps {
  aoNovo: () => void
  aoEditar: () => void
}

// Só os dois botões; quem os usa decide o que cada um abre.
export default function AcoesHabitos({ aoNovo, aoEditar }: AcoesHabitosProps) {
  return (
    <div className={styles.acoes}>
      <button className={styles.novo} type="button" onClick={aoNovo}>
        Novo Hábito
      </button>
      <button className={styles.editar} type="button" onClick={aoEditar}>
        Editar Hábitos
      </button>
    </div>
  )
}
