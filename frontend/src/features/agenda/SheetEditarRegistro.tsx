import BottomSheet from '../../components/ui/BottomSheet'
import type { Registro } from '../calendario/agenda'
import FormularioEntrega from './FormularioEntrega'
import FormularioEvento from './FormularioEvento'

interface SheetEditarRegistroProps {
  // O registro em edição; null com o sheet fechado.
  registro: Registro | null
  aoFechar: () => void
}

// O mesmo sheet do Novo Registro, sem as abas: o tipo já está decidido e os
// campos vêm preenchidos.
export default function SheetEditarRegistro({
  registro,
  aoFechar,
}: SheetEditarRegistroProps) {
  return (
    <BottomSheet
      aberto={registro !== null}
      aoFechar={aoFechar}
      titulo={registro?.tipo === 'ENTREGA' ? 'Editar Entrega' : 'Editar Evento'}
    >
      {registro?.tipo === 'EVENTO' && (
        <FormularioEvento
          key={registro.evento.id}
          existente={registro.evento}
          aoConcluir={aoFechar}
          aoCancelar={aoFechar}
        />
      )}
      {registro?.tipo === 'ENTREGA' && (
        <FormularioEntrega
          key={registro.entrega.id}
          existente={registro.entrega}
          aoConcluir={aoFechar}
          aoCancelar={aoFechar}
        />
      )}
    </BottomSheet>
  )
}
