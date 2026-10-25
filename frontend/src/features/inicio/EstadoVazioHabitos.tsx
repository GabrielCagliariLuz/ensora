import { useId } from 'react'
import { Plus } from 'lucide-react'
import AnelEnsora from '../../components/ui/AnelEnsora'
import AcoesHabitos from './AcoesHabitos'
import { EXEMPLOS_DE_HABITO } from './regras'
import styles from './EstadoVazioHabitos.module.css'

interface EstadoVazioHabitosProps {
  aoNovo: () => void
  // Abre o formulário de hábito já com o nome do exemplo escolhido.
  aoEscolherExemplo: (nome: string) => void
}

// A coluna Today de quem ainda não tem hábito: um convite para criar o primeiro,
// com o botão de sempre e atalhos de exemplo.
export default function EstadoVazioHabitos({
  aoNovo,
  aoEscolherExemplo,
}: EstadoVazioHabitosProps) {
  const idTitulo = useId()

  return (
    <section className={styles.cartao} aria-labelledby={idTitulo}>
      <AnelEnsora className={styles.anel} />
      <h2 id={idTitulo} className={styles.titulo}>
        Comece pelo seu primeiro hábito
      </h2>
      <p className={styles.texto}>
        Hábito é o que você repete: todo dia, ou algumas vezes por semana.
        Marque aqui na Home e acompanhe a consistência no Insights.
      </p>

      <AcoesHabitos aoNovo={aoNovo} />

      <div className={styles.exemplos}>
        <p className={styles.rotulo}>Ou comece por um exemplo</p>
        <ul className={styles.chips}>
          {EXEMPLOS_DE_HABITO.map((nome) => (
            <li key={nome}>
              <button
                className={styles.chip}
                type="button"
                onClick={() => aoEscolherExemplo(nome)}
              >
                <Plus size={14} strokeWidth={1.5} aria-hidden="true" />
                {nome}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
