// O que a URL do Calendar pede ao abrir (?dia=AAAA-MM-DD&tipo=entrega&id=12): o
// dia e, opcionalmente, um registro cujos detalhes já abrem.
export interface PedidoDoCalendario {
  dia: string
  registro: { tipo: 'ENTREGA' | 'EVENTO'; id: number } | null
}

// Só vale uma data que existe: qualquer outra coisa (ausente, mal escrita,
// 2026-02-30) é ignorada e o Calendar abre como de costume.
export function diaDaBusca(valor: string | null): string | null {
  const partes = valor === null ? null : /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor)
  if (partes === null) return null

  const [ano, mes, dia] = partes.slice(1).map(Number)
  const data = new Date(Date.UTC(ano, mes - 1, dia))
  const existe =
    data.getUTCFullYear() === ano &&
    data.getUTCMonth() === mes - 1 &&
    data.getUTCDate() === dia
  return existe ? partes[0] : null
}

// O registro pedido, se tipo e id forem válidos; senão, só o dia vale.
export function pedidoDaBusca(busca: URLSearchParams): PedidoDoCalendario | null {
  const dia = diaDaBusca(busca.get('dia'))
  if (dia === null) return null

  const tipo = busca.get('tipo')?.toUpperCase()
  const id = Number(busca.get('id'))
  const idValido = Number.isInteger(id) && id > 0
  if (idValido && (tipo === 'ENTREGA' || tipo === 'EVENTO')) {
    return { dia, registro: { tipo, id } }
  }
  return { dia, registro: null }
}
