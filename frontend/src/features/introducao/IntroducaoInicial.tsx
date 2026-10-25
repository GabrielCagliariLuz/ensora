import { useState } from 'react'
import { useConta } from '../ajustes/useConta'
import ModalIntroducao from './ModalIntroducao'
import { deveMostrarIntroducao } from './regras'
import { useConcluirIntroducao } from './useConcluirIntroducao'

// O tutorial de primeiro acesso: abre sobre a Home da conta que ainda não o
// viu e, ao encerrar (pular ou concluir), avisa o servidor para não voltar.
export default function IntroducaoInicial() {
  const conta = useConta()
  const concluir = useConcluirIntroducao()
  const [dispensada, setDispensada] = useState(false)

  function encerrar() {
    // Fecha na hora, sem esperar a resposta. Se o aviso falhar (sem conexão, por
    // exemplo), o tutorial só volta no próximo acesso.
    setDispensada(true)
    concluir.mutate()
  }

  return (
    <ModalIntroducao
      aberto={deveMostrarIntroducao(conta.data, dispensada)}
      aoEncerrar={encerrar}
    />
  )
}
