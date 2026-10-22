import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { HabitoInsights } from '../../api/tipos'
import { dataPorExtenso, diaDoMes, rotuloDoMes } from '../../lib/datas'
import CelulaInsights from './CelulaInsights'
import CorpoDaGrade from './CorpoDaGrade'
import LegendaInsights from './LegendaInsights'
import { estadosPorDia } from './celulas'
import { varianteDaCelula } from './estados'
import { diasDoMes, ehFimDeSemana, ehSegunda, inicialDoDia } from './semana'
import styles from './GradeInsightsMes.module.css'

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

interface GradeInsightsMesProps {
  // Mês visível ('AAAA-MM').
  mes: string
  hoje: string
  habitos: HabitoInsights[]
  situacao: 'carregando' | 'erro' | 'vazio' | 'pronto'
  // Os dados na tela são do mês anterior: a grade esmaece até os novos chegarem.
  carregando?: boolean
  aoAnterior: () => void
  aoProximo: () => void
  aoTentarDeNovo: () => void
}

// Grade hábito × dia do mês, para o desktop: uma linha por hábito e uma coluna
// por dia.
export default function GradeInsightsMes({
  mes,
  hoje,
  habitos,
  situacao,
  carregando = false,
  aoAnterior,
  aoProximo,
  aoTentarDeNovo,
}: GradeInsightsMesProps) {
  const dias = diasDoMes(mes)

  return (
    <section className={styles.cartao} aria-label="Grade de hábitos do mês">
      <header className={styles.cabecalho}>
        <button
          className={styles.seta}
          type="button"
          onClick={aoAnterior}
          aria-label="Mês anterior"
        >
          <ChevronLeft size={16} strokeWidth={1.5} />
        </button>
        <h2 className={styles.mes} aria-live="polite">
          {rotuloDoMes(mes)}
        </h2>
        <button
          className={styles.seta}
          type="button"
          onClick={aoProximo}
          aria-label="Próximo mês"
        >
          <ChevronRight size={16} strokeWidth={1.5} />
        </button>
      </header>

      <CorpoDaGrade situacao={situacao} aoTentarDeNovo={aoTentarDeNovo}>
        {/* A chave refaz a tabela a cada mês, e ela surge com um fade. */}
        <div
          key={mes}
          className={
            carregando ? `${styles.rolagem} ${styles.carregando}` : styles.rolagem
          }
        >
          <table className={styles.tabela}>
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
        </div>

        <footer className={styles.legenda}>
          <LegendaInsights />
          <span>
            Hoje · {diaDoMes(hoje)}/{hoje.slice(5, 7)}
          </span>
        </footer>
      </CorpoDaGrade>
    </section>
  )
}
