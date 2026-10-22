import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { CircleAlert } from 'lucide-react'
import LogoEnsora from '../../components/ui/LogoEnsora'
import { CAMINHOS } from '../../lib/caminhos'
import { fusoDoNavegador } from '../../lib/fusos'
import CampoAuth from './CampoAuth'
import {
  errosDoCadastro,
  mensagemErroCadastro,
  validarCadastro,
} from './regrasCadastro'
import type { CampoCadastro, ValoresCadastro } from './regrasCadastro'
import { useCadastrar } from './useCadastrar'
import { useConfiguracaoCadastro } from './useConfiguracaoCadastro'
import styles from './PaginaLogin.module.css'

const ORDEM_DOS_CAMPOS: CampoCadastro[] = [
  'email',
  'senha',
  'confirmacao',
  'codigoConvite',
]

const ID_DO_CAMPO: Record<CampoCadastro, string> = {
  email: 'cadastro-email',
  senha: 'cadastro-senha',
  confirmacao: 'cadastro-confirmacao',
  codigoConvite: 'cadastro-convite',
}

export default function PaginaCadastro() {
  const configuracao = useConfiguracaoCadastro()
  const cadastrar = useCadastrar()
  const [valores, setValores] = useState<ValoresCadastro>({
    email: '',
    senha: '',
    confirmacao: '',
    codigoConvite: '',
  })
  const [errosLocais, setErrosLocais] = useState<
    Partial<Record<CampoCadastro, string>>
  >({})
  // Lido uma vez: o fuso do aparelho vai com o cadastro e a pessoa muda depois,
  // em Configurações, se quiser.
  const [fuso] = useState(fusoDoNavegador)

  const exigeConvite = configuracao.data?.exigeConvite ?? false
  const errosServidor = errosDoCadastro(cadastrar.error)
  const erroDoCampo = (campo: CampoCadastro) =>
    errosLocais[campo] ?? errosServidor[campo]
  const erroGeral =
    cadastrar.isError && Object.keys(errosServidor).length === 0
      ? mensagemErroCadastro(cadastrar.error)
      : null

  // Corrigir um campo apaga só o erro dele.
  function mudar(campo: CampoCadastro, valor: string) {
    setValores((atuais) => ({ ...atuais, [campo]: valor }))
    setErrosLocais((atuais) => ({ ...atuais, [campo]: undefined }))
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const erros = validarCadastro(valores, exigeConvite)
    setErrosLocais(erros)

    const primeiro = ORDEM_DOS_CAMPOS.find((campo) => erros[campo])
    if (primeiro) {
      return document.getElementById(ID_DO_CAMPO[primeiro])?.focus()
    }

    const codigo = valores.codigoConvite.trim()
    cadastrar.mutate({
      email: valores.email.trim(),
      senha: valores.senha,
      fuso,
      ...(exigeConvite && codigo !== '' ? { codigoConvite: codigo } : {}),
    })
  }

  return (
    <main className={styles.pagina}>
      <div className={styles.cartao}>
        <header className={styles.cabecalho}>
          <LogoEnsora />
        </header>

        <div className={styles.corpo}>
          <div>
            <h1 className={styles.titulo}>Criar conta</h1>
            <p className={styles.legenda}>
              Cada conta tem os seus próprios hábitos, entregas e eventos.
            </p>
          </div>

          {configuracao.isPending ? (
            <p className={styles.legenda}>Carregando…</p>
          ) : configuracao.isError ? (
            <div className={styles.formulario}>
              <div className={styles.alerta} role="alert">
                <CircleAlert size={16} strokeWidth={1.5} />
                <span>Não foi possível carregar o cadastro.</span>
              </div>
              <button
                className={styles.botao}
                type="button"
                onClick={() => void configuracao.refetch()}
              >
                Tentar de novo
              </button>
            </div>
          ) : !configuracao.data.habilitado ? (
            <div className={styles.formulario}>
              <div className={styles.alerta} role="alert">
                <CircleAlert size={16} strokeWidth={1.5} />
                <span>O cadastro está fechado no momento.</span>
              </div>
            </div>
          ) : (
            <form className={styles.formulario} onSubmit={enviar} noValidate>
              {erroGeral && (
                <div className={styles.alerta} role="alert">
                  <CircleAlert size={16} strokeWidth={1.5} />
                  <span>{erroGeral}</span>
                </div>
              )}

              <CampoAuth
                id={ID_DO_CAMPO.email}
                rotulo="E-mail"
                tipo="email"
                autoCompletar="username"
                placeholder="voce@exemplo.com"
                valor={valores.email}
                aoMudar={(valor) => mudar('email', valor)}
                erro={erroDoCampo('email')}
              />
              <CampoAuth
                id={ID_DO_CAMPO.senha}
                rotulo="Senha"
                tipo="password"
                autoCompletar="new-password"
                valor={valores.senha}
                aoMudar={(valor) => mudar('senha', valor)}
                erro={erroDoCampo('senha')}
                apoio="De 8 a 72 caracteres."
              />
              <CampoAuth
                id={ID_DO_CAMPO.confirmacao}
                rotulo="Confirmar senha"
                tipo="password"
                autoCompletar="new-password"
                valor={valores.confirmacao}
                aoMudar={(valor) => mudar('confirmacao', valor)}
                erro={erroDoCampo('confirmacao')}
              />
              {exigeConvite && (
                <CampoAuth
                  id={ID_DO_CAMPO.codigoConvite}
                  rotulo="Código de convite"
                  valor={valores.codigoConvite}
                  aoMudar={(valor) => mudar('codigoConvite', valor)}
                  erro={erroDoCampo('codigoConvite')}
                  apoio="Peça o código a quem convidou você."
                />
              )}

              <button
                className={styles.botao}
                type="submit"
                disabled={cadastrar.isPending}
              >
                {cadastrar.isPending ? 'Criando conta…' : 'Criar conta'}
              </button>

              <p className={styles.rodape}>
                Fuso horário: {fuso}. Dá para mudar depois, em Configurações.
              </p>
            </form>
          )}

          <p className={styles.rodape}>
            Já tem conta?{' '}
            <Link className={styles.ligacao} to={CAMINHOS.login}>
              Entrar
            </Link>
          </p>
        </div>
      </div>
    </main>
  )
}
