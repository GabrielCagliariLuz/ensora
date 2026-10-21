import { useId, useState } from 'react'
import BottomSheet from '../../components/ui/BottomSheet'
import ControleSegmentado from '../../components/ui/ControleSegmentado'
import FormularioEntrega from './FormularioEntrega'
import FormularioEvento from './FormularioEvento'
import styles from './SheetNovoRegistro.module.css'

type Aba = 'EVENTO' | 'ENTREGA'

const ABAS: { valor: Aba; rotulo: string }[] = [
  { valor: 'EVENTO', rotulo: 'Evento' },
  { valor: 'ENTREGA', rotulo: 'Entrega' },
]

interface SheetNovoRegistroProps {
  aberto: boolean
  aoFechar: () => void
}

export default function SheetNovoRegistro({
  aberto,
  aoFechar,
}: SheetNovoRegistroProps) {
  return (
    <BottomSheet aberto={aberto} aoFechar={aoFechar} titulo="Novo Registro">
      <ConteudoNovoRegistro aoFechar={aoFechar} />
    </BottomSheet>
  )
}

// Só existe com o sheet aberto (o BottomSheet monta os filhos ao abrir), então
// a aba e os formulários começam limpos a cada abertura.
function ConteudoNovoRegistro({ aoFechar }: { aoFechar: () => void }) {
  const idAbas = useId()
  const [aba, setAba] = useState<Aba>('EVENTO')

  return (
    <>
      <div className={styles.abas}>
        <span id={idAbas} className={styles.apenasLeitor}>
          Tipo de registro
        </span>
        <ControleSegmentado
          idRotulo={idAbas}
          opcoes={ABAS}
          valor={aba}
          aoMudar={setAba}
        />
      </div>

      {/* Os dois formulários ficam montados e o outro só é escondido, para
          trocar de aba não perder o que foi digitado. */}
      <div hidden={aba !== 'EVENTO'}>
        <FormularioEvento aoConcluir={aoFechar} aoCancelar={aoFechar} />
      </div>
      <div hidden={aba !== 'ENTREGA'}>
        <FormularioEntrega aoConcluir={aoFechar} aoCancelar={aoFechar} />
      </div>
    </>
  )
}
