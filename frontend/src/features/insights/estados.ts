import type { EstadoCelula } from '../../api/tipos'

// Como a célula é desenhada. O backend tem 4 estados, e o "descanso/futuro" vira
// duas formas: um pontinho para o dia que já passou (ou é hoje) sem precisar de
// registro (semanal com a semana em andamento ou com a meta batida) e um anel
// fraco para o dia que ainda vem.
export type VarianteCelula =
  | 'CONCLUIDO'
  | 'DESCANSO'
  | 'FUTURO'
  | 'NAO_REALIZADO'
  | 'EM_ABERTO'

// As datas são AAAA-MM-DD, então a comparação de texto é a de data.
export function varianteDaCelula(
  estado: EstadoCelula,
  dia: string,
  hoje: string,
): VarianteCelula {
  if (estado === 'DESCANSO_FUTURO') return dia > hoje ? 'FUTURO' : 'DESCANSO'
  return estado
}

// O nome de cada forma, na legenda e para leitor de tela.
export const ROTULO_VARIANTE: Record<VarianteCelula, string> = {
  CONCLUIDO: 'Concluído',
  DESCANSO: 'Descanso',
  FUTURO: 'Futuro',
  NAO_REALIZADO: 'Não realizado',
  EM_ABERTO: 'Em aberto',
}
