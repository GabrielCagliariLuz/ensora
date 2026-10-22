import { useMesVisivel } from '../calendario/useMesVisivel'
import ConsistenciaDoMes from './ConsistenciaDoMes'
import SeletorDeMes from './SeletorDeMes'

interface ConsistenciaMobileProps {
  hoje: string
}

// A consistência no celular: a grade é semanal, então o mês do bloco tem o seu
// próprio seletor e abre no mês atual.
export default function ConsistenciaMobile({ hoje }: ConsistenciaMobileProps) {
  const { mes, anterior, proximo } = useMesVisivel(hoje)

  return (
    <ConsistenciaDoMes
      mes={mes}
      hoje={hoje}
      seletor={
        <SeletorDeMes mes={mes} aoAnterior={anterior} aoProximo={proximo} />
      }
    />
  )
}
