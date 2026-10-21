import { Trash2 } from 'lucide-react'
import ModalConfirmacao from '../../components/ui/ModalConfirmacao'
import type { Registro } from '../calendario/agenda'
import { mensagemErroExclusao } from '../habitos/regras'
import { useExcluirRegistro } from './useEditarRegistro'
import styles from './ModalExcluirRegistro.module.css'

interface ModalExcluirRegistroProps {
  // O registro a excluir; null com o modal fechado.
  registro: Registro | null
  aoFechar: () => void
}

export default function ModalExcluirRegistro({
  registro,
  aoFechar,
}: ModalExcluirRegistroProps) {
  const excluir = useExcluirRegistro()

  const ehEvento = registro?.tipo === 'EVENTO'
  const titulo =
    registro?.tipo === 'EVENTO'
      ? registro.evento.titulo
      : registro?.entrega.titulo

  function cancelar() {
    excluir.reset()
    aoFechar()
  }

  function confirmar() {
    if (registro === null) return
    excluir.mutate(registro, {
      onSuccess: () => {
        excluir.reset()
        aoFechar()
      },
    })
  }

  return (
    <ModalConfirmacao
      aberto={registro !== null}
      icone={Trash2}
      titulo={ehEvento ? 'Excluir evento?' : 'Excluir entrega?'}
      descricao={
        <>
          Tem certeza de que deseja remover{' '}
          <strong className={styles.nomeDestaque}>{titulo}</strong>? Essa ação não
          pode ser desfeita.
        </>
      }
      rotuloConfirmar="Excluir"
      aoConfirmar={confirmar}
      aoCancelar={cancelar}
      carregando={excluir.isPending}
      erro={excluir.isError ? mensagemErroExclusao(excluir.error) : undefined}
    />
  )
}
