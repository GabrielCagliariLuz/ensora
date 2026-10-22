import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { HabitoInsights } from '../../api/tipos'
import { dataPorExtenso, diaDoMes } from '../../lib/datas'
import CelulaInsights from './CelulaInsights'
import CorpoDaGrade from './CorpoDaGrade'
import LegendaInsights from './LegendaInsights'
import { estadosPorDia } from './celulas'
import { varianteDaCelula } from './estados'
import {
  diasDaSemana,
  ehFimDeSemana,
  ehSegunda,
  inicialDoDia,
  numeroDaSemana,
  rotuloDaSemana,
} from './semana'
import styles from './GradeInsightsSemana.module.css'

// As colunas de sábado e domingo ficam tingidas, a de segunda ganha uma linha
// mais forte à esquerda (a semana se lê de relance) e a de hoje é marcada
// por linhas nos dois lados.
function classesDaColuna(base: string, dia: string, hoje: string): string {
  return [
    base,
    ehSegunda(dia) && styles.inicioDaSemana,
    ehFimDeSemana(dia) && styles.fimDeSemana,
    dia === hoje && styles.colunaDeHoje,
  ]
    .filter(Boolean)
    .join(' ')
}

interface GradeInsightsSemanaProps {
  // A segunda-feira da semana visível (AAAA-MM-DD).
  segunda: string
  hoje: string
  habitos: HabitoInsights[]
  situacao: 'carregando' | 'erro' | 'vazio' | 'pronto'
  // Os dados na tela são de outra semana: a grade esmaece até os novos chegarem.
  carregando?: boolean
  aoAnterior: () => void
  aoProxima: () => void
  aoTentarDeNovo: () => void
}

// Grade hábito × dia da semana, para o celular: 7 colunas. Sem coluna de
// resumo, para o nome do hábito ter o máximo de espaço.
export default function GradeInsightsSemana({
  segunda,
  hoje,
  habitos,
  situacao,
  carregando = false,
  aoAnterior,
  aoProxima,
  aoTentarDeNovo,
}: GradeInsightsSemanaProps) {
  const dias = diasDaSemana(segunda)
  const atual = dias.includes(hoje)

  return (
    <section className={styles.cartao} aria-label="Grade de hábitos da semana">
      <header className={styles.cabecalho}>
        <button
          className={styles.seta}
          type="button"
          onClick={aoAnterior}
          aria-label="Semana anterior"
        >
          <ChevronLeft size={16} strokeWidth={1.5} />
        </button>
        <div className={styles.titulos} aria-live="polite">
          <h3 className={styles.semana}>{rotuloDaSemana(dias)}</h3>
          <span className={styles.numeroDaSemana}>
            Semana {numeroDaSemana(segunda)}
            {atual && ' · atual'}
          </span>
        </div>
        <button
          className={styles.seta}
          type="button"
          onClick={aoProxima}
          aria-label="Próxima semana"
        >
          <ChevronRight size={16} strokeWidth={1.5} />
        </button>
      </header>

      <CorpoDaGrade situacao={situacao} aoTentarDeNovo={aoTentarDeNovo}>
        {/* A chave refaz a tabela a cada semana, e ela surge com um fade. */}
        <table
          key={segunda}
          className={
            carregando
              ? `${styles.tabela} ${styles.carregando}`
              : styles.tabela
          }
        >
          <thead>
            <tr>
              <th className={styles.colunaHabito} scope="col">
                Hábito
              </th>
              {dias.map((dia) => (
                <th
                  key={dia}
                  className={classesDaColuna(styles.dia, dia, hoje)}
                  scope="col"
                  aria-label={dataPorExtenso(dia)}
                >
                  <span className={styles.inicial}>{inicialDoDia(dia)}</span>
                  <span
                    className={
                      dia === hoje
                        ? `${styles.numero} ${styles.hoje}`
                        : styles.numero
                    }
                  >
                    {diaDoMes(dia)}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {habitos.map((habito) => {
              const estados = estadosPorDia(habito)
              return (
                <tr key={habito.id}>
                  <th
                    className={
                      habito.excluido
                        ? `${styles.nome} ${styles.excluido}`
                        : styles.nome
                    }
                    scope="row"
                    title={
                      habito.excluido
                        ? `${habito.nome} (hábito excluído)`
                        : habito.nome
                    }
                  >
                    {habito.nome}
                  </th>
                  {dias.map((dia) => {
                    const estado = estados.get(dia)
                    return (
                      <td key={dia} className={classesDaColuna(styles.celula, dia, hoje)}>
                        {estado && (
                          <CelulaInsights
                            variante={varianteDaCelula(estado, dia, hoje)}
                            descricao={`${habito.nome}, ${dataPorExtenso(dia)}`}
                          />
                        )}
                      </td>
                    )
                  })}
                </tr>
              )
            })}
          </tbody>
        </table>
        <footer className={styles.legenda}>
          <LegendaInsights />
        </footer>
      </CorpoDaGrade>
    </section>
  )
}
