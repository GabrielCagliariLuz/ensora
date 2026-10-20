import { useState } from 'react'
import { CircleAlert } from 'lucide-react'
import type { HabitoNaHome } from '../../api/tipos'
import { useHoje } from '../../lib/useHoje'
import SheetGerenciarHabitos from '../habitos/SheetGerenciarHabitos'
import SheetHabito from '../habitos/SheetHabito'
import AcoesHabitos from './AcoesHabitos'
import CabecalhoHoje from './CabecalhoHoje'
import ListaHabitosDoDia from './ListaHabitosDoDia'
import { mensagemErroMarcacao } from './regras'
import { useInicio } from './useInicio'
import { useMarcarHabito } from './useMarcarHabito'
import styles from './PaginaInicio.module.css'

// Qual sheet de hábitos está aberto, se algum.
type SheetAberto = 'novo' | 'gerenciar' | null

export default function PaginaInicio() {
  const hoje = useHoje()
  const inicio = useInicio(hoje)
  const [aviso, setAviso] = useState<string | null>(null)
  const [sheet, setSheet] = useState<SheetAberto>(null)
  const marcar = useMarcarHabito((erro, marcacao) =>
    setAviso(mensagemErroMarcacao(erro, marcacao.nome)),
  )

  // O alvo é o contrário do que o cartão mostra agora, já com a marcação
  // otimista aplicada. O aviso anterior some no próximo clique.
  function alternar(habito: HabitoNaHome) {
    setAviso(null)
    marcar.mutate({
      id: habito.id,
      nome: habito.nome,
      feito: !habito.feito,
      data: hoje,
    })
  }

  return (
    <>
      <div className={styles.pagina}>
        {/* Coluna Today. No desktop, a segunda coluna fica livre para o Upcoming. */}
        <section className={styles.hoje}>
          <CabecalhoHoje data={hoje} />

          {aviso && (
            <div className={styles.aviso} role="alert">
              <CircleAlert size={16} strokeWidth={1.5} />
              <span>{aviso}</span>
            </div>
          )}

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
            <ListaHabitosDoDia
              habitos={inicio.data.habitos}
              aoAlternar={alternar}
            />
          )}

          {/* Sempre visíveis: com a lista vazia, "Novo Hábito" cria o primeiro. */}
          <AcoesHabitos
            aoNovo={() => setSheet('novo')}
            aoEditar={() => setSheet('gerenciar')}
          />
        </section>
      </div>

      <SheetHabito aberto={sheet === 'novo'} aoFechar={() => setSheet(null)} />
      <SheetGerenciarHabitos
        aberto={sheet === 'gerenciar'}
        aoFechar={() => setSheet(null)}
      />
    </>
  )
}
