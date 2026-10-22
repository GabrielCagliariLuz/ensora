import type { ReactNode } from 'react'
import styles from './CorpoDaGrade.module.css'

interface CorpoDaGradeProps {
  situacao: 'carregando' | 'erro' | 'vazio' | 'futuro' | 'pronto'
  aoTentarDeNovo: () => void
  // Os estados vêm dentro de um cartão próprio (fora do card da grade).
  comCartao?: boolean
  // A grade em si, só quando a situação é "pronto".
  children: ReactNode
}

// O que vai dentro do card, sob o cabeçalho de navegação: o cabeçalho fica
// sempre visível para andar de mês (ou de semana) mesmo sem dados.
export default function CorpoDaGrade({
  situacao,
  aoTentarDeNovo,
  comCartao = false,
  children,
}: CorpoDaGradeProps) {
  if (situacao === 'pronto') return <>{children}</>

  return (
    <div
      className={
        comCartao ? `${styles.estado} ${styles.cartao}` : styles.estado
      }
    >
      {situacao === 'carregando' && <p>Carregando…</p>}
      {situacao === 'vazio' && <p>Nenhum hábito neste período.</p>}
      {situacao === 'futuro' && <p>Este mês ainda não começou.</p>}
      {situacao === 'erro' && (
        <>
          <p>Não foi possível carregar os Insights.</p>
          <button
            className={styles.botao}
            type="button"
            onClick={aoTentarDeNovo}
          >
            Tentar de novo
          </button>
        </>
      )}
    </div>
  )
}
