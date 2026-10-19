import BottomSheet from '../../components/ui/BottomSheet'
import type { Habito } from '../../api/tipos'
import FormularioHabito from './FormularioHabito'

interface SheetHabitoProps {
  aberto: boolean
  aoFechar: () => void
  // Sem hábito abre "Novo Hábito"; com hábito abre "Editar Hábito".
  habito?: Habito
}

// Ponto único de entrada para a Home (criar) e o Gerenciar Hábitos (editar).
export default function SheetHabito({
  aberto,
  aoFechar,
  habito,
}: SheetHabitoProps) {
  return (
    <BottomSheet
      aberto={aberto}
      aoFechar={aoFechar}
      titulo={habito ? 'Editar Hábito' : 'Novo Hábito'}
    >
      <FormularioHabito
        habito={habito}
        aoConcluir={aoFechar}
        aoCancelar={aoFechar}
      />
    </BottomSheet>
  )
}
