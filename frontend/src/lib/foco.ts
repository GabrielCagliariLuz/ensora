// O foco automático abre o teclado do celular junto com a sheet, e o teclado
// cobre o formulário. Só focamos sozinhos quando há mouse ou teclado físico.
export function focoAutomatico(): boolean {
  return !window.matchMedia('(pointer: coarse)').matches
}
