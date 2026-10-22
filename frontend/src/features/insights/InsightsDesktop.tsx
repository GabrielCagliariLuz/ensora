import { useMesVisivel } from '../calendario/useMesVisivel'
import { ordenarPorNome } from './celulas'
import ConsistenciaDoMes from './ConsistenciaDoMes'
import GradeInsightsMes from './GradeInsightsMes'
import { useConsistencia, usePrefetchMesesVizinhos } from './useConsistencia'

interface InsightsDesktopProps {
  hoje: string
}

// Insights no desktop: a grade do mês inteiro, com navegação entre meses, e
// abaixo a consistência e o ritmo do mesmo mês.
export default function InsightsDesktop({ hoje }: InsightsDesktopProps) {
  const { mes, anterior, proximo } = useMesVisivel(hoje)
  const consulta = useConsistencia(mes)
  usePrefetchMesesVizinhos(mes)

  const habitos = ordenarPorNome(consulta.data?.habitos ?? [])
  const situacao = consulta.data
    ? habitos.length === 0
      ? 'vazio'
      : 'pronto'
    : consulta.isError
      ? 'erro'
      : 'carregando'

  return (
    <>
      <GradeInsightsMes
        mes={mes}
        hoje={hoje}
        habitos={habitos}
        situacao={situacao}
        carregando={consulta.isPlaceholderData}
        aoAnterior={anterior}
        aoProximo={proximo}
        aoTentarDeNovo={() => void consulta.refetch()}
      />
      <ConsistenciaDoMes mes={mes} hoje={hoje} />
    </>
  )
}
