import BottomSheet from '../../components/ui/BottomSheet'
import type { Habito } from '../../api/tipos'
import FormularioHabito from './FormularioHabito'

interface SheetHabitoProps {
  aberto: boolean
  aoFechar: () => void
  // Sem hábito abre "Novo Hábito"; com hábito abre "Editar Hábito".
  habito?: Habito
  // Só na criação: nome que o formulário já traz preenchido.
  nomeInicial?: string
}

// Ponto único de entrada para a Home (criar) e o Gerenciar Hábitos (editar).
export default function SheetHabito({
  aberto,
  aoFechar,
  habito,
  nomeInicial,
}: SheetHabitoProps) {
  return (
    <BottomSheet
      aberto={aberto}
      aoFechar={aoFechar}
      titulo={habito ? 'Editar Hábito' : 'Novo Hábito'}
    >
      <FormularioHabito
        habito={habito}
        nomeInicial={nomeInicial}
        aoConcluir={aoFechar}
        aoCancelar={aoFechar}
      />
    </BottomSheet>
  )
}
