import { useState } from 'react'
import {
  ajustarHorario,
  MENSAGEM_HORARIO_INVALIDO,
  normalizarHorario,
} from '../../lib/horarios'
import CampoTexto from './CampoTexto'

interface CampoHorarioProps {
  id: string
  rotulo: string
  valor: string
  aoMudar: (valor: string) => void
  opcional?: boolean
  placeholder?: string
  erro?: string
  apoio?: string
  // Com o campo vazio, a primeira seta parte deste horário (por exemplo, o
  // início ao ajustar o término).
  aPartirDe?: string
}

// Horário digitado como texto (9, 930, 9:30) e ajustado com as setas, de 15 em
// 15 minutos, como no Google Calendar. Ao sair do campo, o que é válido vira
// HH:mm e o que não é fica marcado: só passa se estiver certo.
export default function CampoHorario({
  valor,
  aoMudar,
  erro,
  aPartirDe,
  ...resto
}: CampoHorarioProps) {
  const [saiu, setSaiu] = useState(false)

  // O erro do formulário vale primeiro. Fora dele, o campo avisa sozinho
  // depois que a pessoa sai dele com um horário inválido.
  const erroDoCampo =
    erro ??
    (saiu && normalizarHorario(valor) === null
      ? MENSAGEM_HORARIO_INVALIDO
      : undefined)

  return (
    <CampoTexto
      {...resto}
      valor={valor}
      aoMudar={aoMudar}
      erro={erroDoCampo}
      modoEntrada="numeric"
      maxLength={5}
      aoTeclar={(evento) => {
        if (evento.key !== 'ArrowUp' && evento.key !== 'ArrowDown') return
        evento.preventDefault()
        aoMudar(
          ajustarHorario(valor, evento.key === 'ArrowUp' ? 1 : -1, aPartirDe),
        )
      }}
      aoSair={() => {
        setSaiu(true)
        const normalizado = normalizarHorario(valor)
        if (normalizado) aoMudar(normalizado)
      }}
    />
  )
}
