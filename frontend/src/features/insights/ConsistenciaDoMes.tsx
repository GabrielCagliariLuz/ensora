import type { ReactNode } from 'react'
import { somarMeses } from '../calendario/grade'
import { montarCartoes } from './cartoes'
import CartaoHabito from './CartaoHabito'
import CorpoDaGrade from './CorpoDaGrade'
import RitmoDoMes from './RitmoDoMes'
import RitmoPorDiaDaSemana from './RitmoPorDiaDaSemana'
import { barrasSemanais, ritmoPorDiaDaSemana } from './ritmo'
import { useConsistencia } from './useConsistencia'
import { usePrefetchRitmoVizinho, useRitmo } from './useRitmo'
import styles from './ConsistenciaDoMes.module.css'

interface ConsistenciaDoMesProps {
  // Mês visível ('AAAA-MM'): no desktop é o mesmo da grade; no mobile, o do
  // seletor próprio.
  mes: string
  hoje: string
  // Fica entre o título e o conteúdo (o seletor de mês do mobile).
  seletor?: ReactNode
}

// O bloco "Consistência do mês": ritmo médio com as barras S1–S4, o ritmo por
// dia da semana e um cartão com o anel de consistência de cada hábito. Une três
// consultas: o ritmo do mês, a consistência do mês e a do mês anterior (só para
// o delta de cada hábito, que aparece quando ela chega).
export default function ConsistenciaDoMes({
  mes,
  hoje,
  seletor,
}: ConsistenciaDoMesProps) {
  const { intervalo, consulta: ritmo } = useRitmo(mes, hoje)
  usePrefetchRitmoVizinho(mes, hoje)
  const atual = useConsistencia(mes)
  const anterior = useConsistencia(somarMeses(mes, -1))

  const futuro = intervalo === null
  const habitos = atual.data?.habitos ?? []
  const pronto = atual.data !== undefined && ritmo.data !== undefined

  const situacao = futuro
    ? 'futuro'
    : pronto
      ? habitos.length === 0
        ? 'vazio'
        : 'pronto'
      : atual.isError || ritmo.isError
        ? 'erro'
        : 'carregando'

  const cartoes = montarCartoes(habitos, anterior.data?.habitos)
  const medias = ritmoPorDiaDaSemana(ritmo.data?.dias ?? [])
  const barras = barrasSemanais(mes, ritmo.data, hoje)
  const carregando = atual.isPlaceholderData || ritmo.isPlaceholderData

  return (
    <section className={styles.secao}>
      <h2 className={styles.titulo}>Consistência do mês</h2>
      {seletor}

      <CorpoDaGrade
        situacao={situacao}
        aoTentarDeNovo={() => {
          void atual.refetch()
          void ritmo.refetch()
        }}
        comCartao
      >
        {/* A chave refaz o bloco a cada mês, e ele surge com um fade. */}
        <div
          key={mes}
          className={
            carregando ? `${styles.corpo} ${styles.carregando}` : styles.corpo
          }
        >
          <div className={styles.ritmo}>
            <RitmoDoMes
              mes={mes}
              ritmoMedio={ritmo.data?.ritmoMedio}
              barras={barras}
            />
            <RitmoPorDiaDaSemana medias={medias} />
          </div>

          <ul className={styles.cartoes}>
            {cartoes.map((cartao) => (
              <li key={cartao.id}>
                <CartaoHabito cartao={cartao} />
              </li>
            ))}
          </ul>
        </div>
      </CorpoDaGrade>
    </section>
  )
}
