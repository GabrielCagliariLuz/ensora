import type { Entrega } from '../../api/tipos'
import BottomSheet from '../../components/ui/BottomSheet'
import type { Registro } from './agenda'
import DetalhesRegistro from './DetalhesRegistro'
import styles from './SheetDetalhesRegistro.module.css'

interface SheetDetalhesRegistroProps {
  // O registro aberto; null com o sheet fechado.
  registro: Registro | null
  agora: Date
  aoFechar: () => void
  aoAlternarEntrega: (entrega: Entrega) => void
  aoEditar: (registro: Registro) => void
  aoExcluir: (registro: Registro) => void
}

// Os detalhes do popover do desktop, num bottom sheet para o toque, com Editar e
// Excluir (no mobile não há arrastar nem popover).
export default function SheetDetalhesRegistro({
  registro,
  agora,
  aoFechar,
  aoAlternarEntrega,
  aoEditar,
  aoExcluir,
}: SheetDetalhesRegistroProps) {
  return (
    <BottomSheet
      aberto={registro !== null}
      aoFechar={aoFechar}
      titulo={registro?.tipo === 'ENTREGA' ? 'Entrega' : 'Evento'}
    >
      {registro !== null && (
        <>
          <DetalhesRegistro
            registro={registro}
            agora={agora}
            aoAlternarEntrega={aoAlternarEntrega}
          />
          <div className={styles.acoes}>
            <button
              className={styles.botao}
              type="button"
              onClick={() => aoEditar(registro)}
            >
              Editar
            </button>
            <button
              className={`${styles.botao} ${styles.excluir}`}
              type="button"
              onClick={() => aoExcluir(registro)}
            >
              Excluir
            </button>
          </div>
        </>
      )}
    </BottomSheet>
  )
}
