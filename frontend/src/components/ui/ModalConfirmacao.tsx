import { useId } from 'react'
import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'
import CaixaDialogo from './CaixaDialogo'
import styles from './ModalConfirmacao.module.css'

interface ModalConfirmacaoProps {
  aberto: boolean
  titulo: string
  // ReactNode para o chamador poder destacar um trecho (ex.: o nome em mono).
  descricao: ReactNode
  icone?: LucideIcon
  rotuloConfirmar: string
  rotuloCancelar?: string
  aoConfirmar: () => void
  aoCancelar: () => void
  // Ação em andamento: trava os botões, o Esc e o clique no fundo.
  carregando?: boolean
}

// Sempre centralizado, no mobile e no desktop. Não conhece o domínio: o texto
// e as ações vêm de quem usa.
export default function ModalConfirmacao({
  aberto,
  titulo,
  descricao,
  icone: Icone,
  rotuloConfirmar,
  rotuloCancelar = 'Cancelar',
  aoConfirmar,
  aoCancelar,
  carregando = false,
}: ModalConfirmacaoProps) {
  const idTitulo = useId()

  return (
    <CaixaDialogo
      aberto={aberto}
      aoFechar={carregando ? () => {} : aoCancelar}
      idTitulo={idTitulo}
      className={styles.modal}
    >
      <div className={styles.conteudo}>
        {Icone && (
          <div className={styles.icone}>
            <Icone size={22} strokeWidth={1.5} />
          </div>
        )}
        <h2 id={idTitulo} className={styles.titulo}>
          {titulo}
        </h2>
        <div className={styles.descricao}>{descricao}</div>
        <div className={styles.acoes}>
          <button
            className={styles.cancelar}
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
          >
            {rotuloCancelar}
          </button>
          <button
            className={styles.confirmar}
            type="button"
            onClick={aoConfirmar}
            disabled={carregando}
          >
            {carregando ? 'Aguarde…' : rotuloConfirmar}
          </button>
        </div>
      </div>
    </CaixaDialogo>
  )
}
