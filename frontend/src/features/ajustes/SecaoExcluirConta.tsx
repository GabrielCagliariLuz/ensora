import { useState } from 'react'
import { Trash2 } from 'lucide-react'
import CampoTexto from '../../components/ui/CampoTexto'
import ModalConfirmacao from '../../components/ui/ModalConfirmacao'
import CartaoAjuste from './CartaoAjuste'
import { mensagemErroExclusao } from './regras'
import { useExcluirConta } from './useExcluirConta'
import styles from './CartaoAjuste.module.css'
import modal from './ModalExcluirConta.module.css'

// A exclusão apaga a conta e tudo o que é dela, sem volta: pede a senha num
// modal antes de seguir.
export default function SecaoExcluirConta() {
  const excluir = useExcluirConta()
  const [aberto, setAberto] = useState(false)
  const [senha, setSenha] = useState('')

  function fechar() {
    excluir.reset()
    setSenha('')
    setAberto(false)
  }

  function confirmar() {
    if (senha === '' || excluir.isPending) return
    excluir.mutate(senha)
  }

  return (
    <CartaoAjuste titulo="Excluir conta" perigo>
      <p className={styles.texto}>
        Apaga a sua conta e tudo o que é dela: hábitos e registros, entregas e
        eventos. Não dá para desfazer.
      </p>
      <div className={styles.acoes}>
        <button
          className={styles.botaoPerigo}
          type="button"
          onClick={() => setAberto(true)}
        >
          <Trash2 size={16} strokeWidth={1.5} />
          Excluir conta
        </button>
      </div>

      <ModalConfirmacao
        aberto={aberto}
        icone={Trash2}
        titulo="Excluir sua conta?"
        descricao={
          <>
            <p className={modal.texto}>
              Todos os seus dados serão apagados de vez. Para confirmar, digite a
              sua senha.
            </p>
            <div className={modal.campo}>
              <CampoTexto
                id="excluir-conta-senha"
                rotulo="Senha"
                tipo="password"
                autoCompletar="current-password"
                valor={senha}
                aoMudar={setSenha}
                focoInicial
                aoTeclar={(evento) => {
                  if (evento.key === 'Enter') confirmar()
                }}
              />
            </div>
          </>
        }
        rotuloConfirmar="Excluir conta"
        aoConfirmar={confirmar}
        aoCancelar={fechar}
        carregando={excluir.isPending}
        confirmarDesabilitado={senha === ''}
        erro={excluir.isError ? mensagemErroExclusao(excluir.error) : undefined}
      />
    </CartaoAjuste>
  )
}
