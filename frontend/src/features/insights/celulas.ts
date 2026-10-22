import type { Consistencia, EstadoCelula, HabitoInsights } from '../../api/tipos'

// Estado de cada dia em que o hábito existia. Dia ausente do mapa não tem
// célula: o hábito ainda não existia (ou já tinha sido excluído) e a grade fica
// em branco.
export function estadosPorDia(
  habito: HabitoInsights,
): Map<string, EstadoCelula> {
  return new Map(habito.dias.map((celula) => [celula.data, celula.estado]))
}

// Em ordem alfabética, sem diferenciar maiúsculas nem acentos: hábitos de nome
// parecido ("Estudar Inglês" e "Estudar Programação") ficam juntos. Nomes iguais
// mantêm a ordem de criação.
export function ordenarPorNome(habitos: HabitoInsights[]): HabitoInsights[] {
  return [...habitos].sort(
    (a, b) =>
      a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' }) ||
      a.id - b.id,
  )
}

// Junta as respostas de mais de um mês (uma semana que cruza a virada do mês
// busca os dois) num hábito por id, com os dias e as semanas de todas elas, sem
// repetir nada. A ordem é a de criação (id crescente).
export function mesclarHabitos(consistencias: Consistencia[]): HabitoInsights[] {
  const porId = new Map<number, HabitoInsights>()

  for (const { habitos } of consistencias) {
    for (const habito of habitos) {
      const atual = porId.get(habito.id)
      if (atual === undefined) {
        porId.set(habito.id, {
          ...habito,
          dias: [...habito.dias],
          semanas: habito.semanas ? [...habito.semanas] : undefined,
        })
        continue
      }

      const datas = new Set(atual.dias.map((celula) => celula.data))
      atual.dias.push(...habito.dias.filter((celula) => !datas.has(celula.data)))

      if (habito.semanas) {
        const semanas = atual.semanas ?? []
        const inicios = new Set(semanas.map((semana) => semana.de))
        atual.semanas = [
          ...semanas,
          ...habito.semanas.filter((semana) => !inicios.has(semana.de)),
        ]
      }
    }
  }

  return [...porId.values()].sort((a, b) => a.id - b.id)
}
