export const MENSAGEM_HORARIO_INVALIDO = 'Horário inválido. Use o formato 09:30.'

const PASSO_MINUTOS = 15
const ULTIMO_MINUTO_DO_DIA = 23 * 60 + 45

function doisDigitos(numero: number): string {
  return String(numero).padStart(2, '0')
}

function paraMinutos(horario: string): number {
  const [horas, minutos] = horario.split(':').map(Number)
  return horas * 60 + minutos
}

function deMinutos(total: number): string {
  return `${doisDigitos(Math.floor(total / 60))}:${doisDigitos(total % 60)}`
}

// Texto digitado vira HH:mm. Aceita 9, 930, 0930, 9:30 e 09:30. Devolve ''
// para texto vazio (nada digitado) e null quando não é um horário válido.
export function normalizarHorario(texto: string): string | null {
  const digitado = texto.trim()
  if (digitado === '') return ''

  let horas: number
  let minutos: number
  const soHora = /^(\d{1,2})$/.exec(digitado)
  const comDoisPontos = /^(\d{1,2}):(\d{2})$/.exec(digitado)
  const colado = /^\d{3,4}$/.exec(digitado)

  if (soHora) {
    horas = Number(soHora[1])
    minutos = 0
  } else if (comDoisPontos) {
    horas = Number(comDoisPontos[1])
    minutos = Number(comDoisPontos[2])
  } else if (colado) {
    horas = Number(digitado.slice(0, -2))
    minutos = Number(digitado.slice(-2))
  } else {
    return null
  }

  if (horas > 23 || minutos > 59) return null
  return `${doisDigitos(horas)}:${doisDigitos(minutos)}`
}

// Seta para cima (passo 1) ou para baixo (passo -1): vai para o próximo ou
// anterior múltiplo de 15 minutos, como no Google Calendar. 09:00 vira 09:15
// ou 08:45, e 09:07 vira 09:15 ou 09:00. Não passa de 00:00 nem de 23:45
// (o evento não atravessa a meia-noite).
// Campo vazio: parte de `aPartirDe` (por exemplo, o início ao ajustar o
// término) e, sem ele, do quarto de hora mais próximo de agora.
export function ajustarHorario(
  texto: string,
  passo: 1 | -1,
  aPartirDe = '',
  agora: Date = new Date(),
): string {
  const base = normalizarHorario(texto) || normalizarHorario(aPartirDe)

  let minutos: number
  if (base) {
    const atual = paraMinutos(base)
    minutos =
      passo === 1
        ? (Math.floor(atual / PASSO_MINUTOS) + 1) * PASSO_MINUTOS
        : (Math.ceil(atual / PASSO_MINUTOS) - 1) * PASSO_MINUTOS
  } else {
    const agoraEmMinutos = agora.getHours() * 60 + agora.getMinutes()
    minutos = Math.round(agoraEmMinutos / PASSO_MINUTOS) * PASSO_MINUTOS
  }

  return deMinutos(Math.min(ULTIMO_MINUTO_DO_DIA, Math.max(0, minutos)))
}
