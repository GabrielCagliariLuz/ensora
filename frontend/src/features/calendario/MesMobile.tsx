import { ChevronLeft, ChevronRight } from 'lucide-react'
import { dataPorExtenso, numeroDoDia, rotuloDoMes } from '../../lib/datas'
import { registrosDoDia } from './agenda'
import type { RegistrosDoDia } from './agenda'
import { gradeDoMes, mesDoDia } from './grade'
import { pontosDoDia } from './pontos'
import styles from './MesMobile.module.css'

const LETRAS_DOS_DIAS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S']

interface MesMobileProps {
  // Mês visível ('AAAA-MM').
  mes: string
  hoje: string
  agora: Date
  // O dia da lista, com selo escuro.
  selecionado: string
  // Registros por dia (AAAA-MM-DD), de agruparPorDia.
  dias: Map<string, RegistrosDoDia>
  // Os dados na tela são do mês anterior: o cartão esmaece até os novos chegarem.
  carregando?: boolean
  aoAnterior: () => void
  aoProximo: () => void
  aoEscolherDia: (dia: string) => void
}

// Cartão do mês do Calendar mobile: pontos coloridos sob os dias com registros.
export default function MesMobile({
  mes,
  hoje,
  agora,
  selecionado,
  dias,
  carregando = false,
  aoAnterior,
  aoProximo,
  aoEscolherDia,
}: MesMobileProps) {
  return (
    <section className={styles.cartao} aria-label="Calendário do mês">
      <header className={styles.cabecalho}>
        <h2 className={styles.mes} aria-live="polite">
          {rotuloDoMes(mes)}
        </h2>
        <div className={styles.setas}>
          <button
            className={styles.seta}
            type="button"
            onClick={aoAnterior}
            aria-label="Mês anterior"
          >
            <ChevronLeft size={16} strokeWidth={1.5} />
          </button>
          <button
            className={styles.seta}
            type="button"
            onClick={aoProximo}
            aria-label="Próximo mês"
          >
            <ChevronRight size={16} strokeWidth={1.5} />
          </button>
        </div>
      </header>

      <div className={styles.semana} aria-hidden="true">
        {LETRAS_DOS_DIAS.map((letra, indice) => (
          <span key={indice}>{letra}</span>
        ))}
      </div>

      {/* A chave refaz os dias a cada mês, e eles surgem com um fade. */}
      <div
        key={mes}
        className={
          carregando ? `${styles.dias} ${styles.carregando}` : styles.dias
        }
      >
        {gradeDoMes(mes).map((dia) => {
          const pontos = pontosDoDia(registrosDoDia(dias.get(dia)), agora)
          const classes = [
            styles.dia,
            mesDoDia(dia) !== mes && styles.foraDoMes,
            dia === hoje && styles.hoje,
            dia === selecionado && styles.selecionado,
          ]
            .filter(Boolean)
            .join(' ')

          return (
            <button
              key={dia}
              className={classes}
              type="button"
              onClick={() => aoEscolherDia(dia)}
              aria-pressed={dia === selecionado}
              aria-label={dataPorExtenso(dia)}
            >
              <span className={styles.numero}>{numeroDoDia(dia)}</span>
              <span className={styles.pontos} aria-hidden="true">
                {pontos.map((cor, indice) => (
                  <span
                    key={indice}
                    className={styles.ponto}
                    style={{ background: cor }}
                  />
                ))}
              </span>
            </button>
          )
        })}
      </div>
    </section>
  )
}
