import { mesDoDia } from './grade'

// O dia que a lista mostra ao abrir um mês: hoje, se for o mês atual; senão, o
// dia 1.
export function diaInicialDoMes(mes: string, hoje: string): string {
  return mesDoDia(hoje) === mes ? hoje : `${mes}-01`
}

// O dia escolhido vale enquanto estiver no mês visível; ao trocar de mês (ou sem
// escolha), volta ao dia inicial dele.
export function diaSelecionado(
  escolhido: string | null,
  mes: string,
  hoje: string,
): string {
  return escolhido !== null && mesDoDia(escolhido) === mes
    ? escolhido
    : diaInicialDoMes(mes, hoje)
}
