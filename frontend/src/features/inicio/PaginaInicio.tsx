import { useHoje } from '../../lib/useHoje'
import CabecalhoHoje from './CabecalhoHoje'
import ListaHabitosDoDia from './ListaHabitosDoDia'
import { useInicio } from './useInicio'
import styles from './PaginaInicio.module.css'

export default function PaginaInicio() {
  const hoje = useHoje()
  const inicio = useInicio(hoje)

  return (
    <div className={styles.pagina}>
      {/* Coluna Today. No desktop, a segunda coluna fica livre para o Upcoming. */}
      <section className={styles.hoje}>
        <CabecalhoHoje data={hoje} />

        {inicio.isPending ? (
          <p className={styles.estado}>Carregando…</p>
        ) : inicio.isError ? (
          <div className={styles.estado}>
            <p>Não foi possível carregar seus hábitos.</p>
            <button
              className={styles.botao}
              type="button"
              onClick={() => void inicio.refetch()}
            >
              Tentar de novo
            </button>
          </div>
        ) : inicio.data.habitos.length === 0 ? (
          <p className={styles.estado}>Nenhum hábito ainda.</p>
        ) : (
          <ListaHabitosDoDia habitos={inicio.data.habitos} />
        )}
      </section>
    </div>
  )
}
