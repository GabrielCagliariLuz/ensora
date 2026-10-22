import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import BlocoAviso from '../../components/ui/BlocoAviso'
import CampoTexto from '../../components/ui/CampoTexto'
import CartaoAjuste from './CartaoAjuste'
import {
  errosDaTrocaDeSenha,
  mensagemErroTrocaDeSenha,
  validarTrocaDeSenha,
} from './regras'
import type { CampoTrocaDeSenha, ValoresTrocaDeSenha } from './regras'
import { useAlterarSenha } from './useAlterarSenha'
import styles from './CartaoAjuste.module.css'

const ORDEM_DOS_CAMPOS: CampoTrocaDeSenha[] = [
  'senhaAtual',
  'novaSenha',
  'confirmacao',
]

const VAZIO: ValoresTrocaDeSenha = {
  senhaAtual: '',
  novaSenha: '',
  confirmacao: '',
}

export default function SecaoSenha() {
  const id = useId()
  const idCampo = (campo: CampoTrocaDeSenha) => `${id}-${campo}`
  const trocar = useAlterarSenha()
  const [valores, setValores] = useState<ValoresTrocaDeSenha>(VAZIO)
  const [errosLocais, setErrosLocais] = useState<
    Partial<Record<CampoTrocaDeSenha, string>>
  >({})

  const errosServidor = errosDaTrocaDeSenha(trocar.error)
  const erroDoCampo = (campo: CampoTrocaDeSenha) =>
    errosLocais[campo] ?? errosServidor[campo]
  const erroGeral =
    trocar.isError && Object.keys(errosServidor).length === 0
      ? mensagemErroTrocaDeSenha(trocar.error)
      : null

  // Corrigir um campo apaga só o erro dele.
  function mudar(campo: CampoTrocaDeSenha, valor: string) {
    setValores((atuais) => ({ ...atuais, [campo]: valor }))
    setErrosLocais((atuais) => ({ ...atuais, [campo]: undefined }))
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const erros = validarTrocaDeSenha(valores)
    setErrosLocais(erros)

    const primeiro = ORDEM_DOS_CAMPOS.find((campo) => erros[campo])
    if (primeiro) return document.getElementById(idCampo(primeiro))?.focus()

    trocar.mutate({
      senhaAtual: valores.senhaAtual,
      novaSenha: valores.novaSenha,
    })
  }

  return (
    <CartaoAjuste titulo="Senha">
      <p className={styles.texto}>
        Ao trocar a senha você sai de todos os aparelhos e entra de novo com a
        nova.
      </p>
      <form className={styles.formulario} onSubmit={enviar} noValidate>
        {erroGeral && <BlocoAviso tom="erro">{erroGeral}</BlocoAviso>}

        <CampoTexto
          id={idCampo('senhaAtual')}
          rotulo="Senha atual"
          tipo="password"
          autoCompletar="current-password"
          valor={valores.senhaAtual}
          aoMudar={(valor) => mudar('senhaAtual', valor)}
          erro={erroDoCampo('senhaAtual')}
        />
        <CampoTexto
          id={idCampo('novaSenha')}
          rotulo="Nova senha"
          tipo="password"
          autoCompletar="new-password"
          valor={valores.novaSenha}
          aoMudar={(valor) => mudar('novaSenha', valor)}
          erro={erroDoCampo('novaSenha')}
          apoio="De 8 a 72 caracteres."
        />
        <CampoTexto
          id={idCampo('confirmacao')}
          rotulo="Confirmar nova senha"
          tipo="password"
          autoCompletar="new-password"
          valor={valores.confirmacao}
          aoMudar={(valor) => mudar('confirmacao', valor)}
          erro={erroDoCampo('confirmacao')}
        />

        <div className={styles.acoes}>
          <button
            className={styles.botaoPrimario}
            type="submit"
            disabled={trocar.isPending}
          >
            {trocar.isPending ? 'Trocando…' : 'Trocar senha'}
          </button>
        </div>
      </form>
    </CartaoAjuste>
  )
}
