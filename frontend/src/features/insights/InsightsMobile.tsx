import { mesclarHabitos, ordenarPorNome } from './celulas'
import GradeInsightsSemana from './GradeInsightsSemana'
import { diasDaSemana, mesesDaSemana } from './semana'
import { useConsistencia, usePrefetchMesesVizinhos } from './useConsistencia'
import { useSemanaVisivel } from './useSemanaVisivel'

interface InsightsMobileProps {
  hoje: string
}

// Insights no celular: a grade de uma semana (segunda a domingo), com
// navegação de semana em semana. Uma semana que cruza a virada do mês usa as
// respostas dos dois meses; com o mesmo mês nas duas chamadas, a consulta é uma
// só.
export default function InsightsMobile({ hoje }: InsightsMobileProps) {
  const { segunda, anterior, proxima } = useSemanaVisivel(hoje)
  const dias = diasDaSemana(segunda)
  const meses = mesesDaSemana(dias)
  const primeiro = useConsistencia(meses[0])
  const segundo = useConsistencia(meses[1] ?? meses[0])
  usePrefetchMesesVizinhos(meses[0])

  const cruzaMes = meses.length === 2
  const consultas = cruzaMes ? [primeiro, segundo] : [primeiro]
  const respostas = consultas.flatMap((consulta) =>
    consulta.data ? [consulta.data] : [],
  )
  const completo = respostas.length === consultas.length

  const habitos = completo ? ordenarPorNome(mesclarHabitos(respostas)) : []
  const situacao = completo
    ? habitos.length === 0
      ? 'vazio'
      : 'pronto'
    : consultas.some((consulta) => consulta.isError)
      ? 'erro'
      : 'carregando'

  return (
    <GradeInsightsSemana
      segunda={segunda}
      hoje={hoje}
      habitos={habitos}
      situacao={situacao}
      carregando={consultas.some((consulta) => consulta.isPlaceholderData)}
      aoAnterior={anterior}
      aoProxima={proxima}
      aoTentarDeNovo={() =>
        consultas.forEach((consulta) => void consulta.refetch())
      }
    />
  )
}
