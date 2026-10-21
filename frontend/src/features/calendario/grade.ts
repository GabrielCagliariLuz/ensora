import { diasEntre, somarDias } from '../../lib/datas'

// O mês é a chave 'AAAA-MM'. A grade vai de domingo a sábado e inclui dias dos
// meses vizinhos para completar as semanas.

export function mesDoDia(iso: string): string {
  return iso.slice(0, 7)
}

export function somarMeses(mes: string, quantidade: number): string {
  const [ano, numero] = mes.split('-').map(Number)
  const total = ano * 12 + (numero - 1) + quantidade
  const novoAno = Math.floor(total / 12)
  const novoMes = (total % 12) + 1
  return `${novoAno}-${String(novoMes).padStart(2, '0')}`
}

// Todos os dias da grade do mês, em ordem: 35 (5 semanas) ou 42 (6 semanas).
export function gradeDoMes(mes: string): string[] {
  const primeiro = `${mes}-01`
  const [ano, numero] = mes.split('-').map(Number)
  const diaDaSemanaDoPrimeiro = new Date(ano, numero - 1, 1).getDay() // 0 = domingo
  const inicio = somarDias(primeiro, -diaDaSemanaDoPrimeiro)
  const ultimo = somarDias(`${somarMeses(mes, 1)}-01`, -1)

  const semanas = Math.ceil((diasEntre(inicio, ultimo) + 1) / 7)
  return Array.from({ length: semanas * 7 }, (_, indice) =>
    somarDias(inicio, indice),
  )
}

// Intervalo que vai no GET /calendario: o primeiro e o último dia da grade.
export function intervaloDaGrade(mes: string): { de: string; ate: string } {
  const grade = gradeDoMes(mes)
  return { de: grade[0], ate: grade[grade.length - 1] }
}
