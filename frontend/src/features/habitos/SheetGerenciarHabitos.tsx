import { useState } from 'react'
import { Trash2 } from 'lucide-react'
import type { Habito } from '../../api/tipos'
import BottomSheet from '../../components/ui/BottomSheet'
import ModalConfirmacao from '../../components/ui/ModalConfirmacao'
import ListaHabitos from './ListaHabitos'
import { mensagemErroExclusao } from './regras'
import SheetHabito from './SheetHabito'
import { useExcluirHabito } from './useExcluirHabito'
import { useHabitos } from './useHabitos'
import styles from './SheetGerenciarHabitos.module.css'

interface SheetGerenciarHabitosProps {
  aberto: boolean
  aoFechar: () => void
}

// Os três diálogos ficam lado a lado, não aninhados: cada <dialog> modal abre
// na camada de cima, e o Esc fecha só o último aberto.
export default function SheetGerenciarHabitos({
  aberto,
  aoFechar,
}: SheetGerenciarHabitosProps) {
  const habitos = useHabitos(aberto)
  const excluir = useExcluirHabito()
  const [editando, setEditando] = useState<Habito | null>(null)
  const [excluindo, setExcluindo] = useState<Habito | null>(null)

  function pedirExclusao(habito: Habito) {
    excluir.reset()
    setExcluindo(habito)
  }

  function cancelarExclusao() {
    excluir.reset()
    setExcluindo(null)
  }

  function confirmarExclusao() {
    if (excluindo === null) return
    excluir.mutate(excluindo.id, { onSuccess: () => setExcluindo(null) })
  }

  return (
    <>
      <BottomSheet aberto={aberto} aoFechar={aoFechar} titulo="Gerenciar Hábitos">
        {habitos.isPending ? (
          <p className={styles.estado}>Carregando…</p>
        ) : habitos.isError ? (
          <div className={styles.estado}>
            <p>Não foi possível carregar os hábitos.</p>
            <button
              className={styles.botao}
              type="button"
              onClick={() => void habitos.refetch()}
            >
              Tentar de novo
            </button>
          </div>
        ) : habitos.data.length === 0 ? (
          <p className={styles.estado}>Nenhum hábito ainda.</p>
        ) : (
          <ListaHabitos
            habitos={habitos.data}
            aoEditar={setEditando}
            aoExcluir={pedirExclusao}
          />
        )}
      </BottomSheet>

      <SheetHabito
        aberto={editando !== null}
        aoFechar={() => setEditando(null)}
        habito={editando ?? undefined}
      />

      <ModalConfirmacao
        aberto={excluindo !== null}
        icone={Trash2}
        titulo="Excluir hábito?"
        descricao={
          <>
            Tem certeza de que deseja remover{' '}
            <strong className={styles.nomeDestaque}>{excluindo?.nome}</strong>? O
            hábito será removido das telas.
          </>
        }
        rotuloConfirmar="Excluir"
        aoConfirmar={confirmarExclusao}
        aoCancelar={cancelarExclusao}
        carregando={excluir.isPending}
        erro={excluir.isError ? mensagemErroExclusao(excluir.error) : undefined}
      />
    </>
  )
}
