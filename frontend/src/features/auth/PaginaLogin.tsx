import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { CircleAlert } from 'lucide-react'
import LogoEnsora from '../../components/ui/LogoEnsora'
import { mensagemErroLogin } from './mensagemErroLogin'
import { useEntrar } from './useEntrar'
import styles from './PaginaLogin.module.css'

interface ErrosCampos {
  email?: string
  senha?: string
}

export default function PaginaLogin() {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [errosCampos, setErrosCampos] = useState<ErrosCampos>({})
  const campoEmail = useRef<HTMLInputElement>(null)
  const campoSenha = useRef<HTMLInputElement>(null)
  const login = useEntrar()

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const emailLimpo = email.trim()
    const erros: ErrosCampos = {}
    if (emailLimpo === '') erros.email = 'Informe o e-mail.'
    if (senha === '') erros.senha = 'Informe a senha.'
    setErrosCampos(erros)

    if (erros.email) return campoEmail.current?.focus()
    if (erros.senha) return campoSenha.current?.focus()

    login.mutate(
      { email: emailLimpo, senha },
      { onError: () => campoSenha.current?.select() },
    )
  }

  return (
    <main className={styles.pagina}>
      <div className={styles.cartao}>
        <header className={styles.cabecalho}>
          <LogoEnsora />
        </header>

        <div className={styles.corpo}>
          <div>
            <h1 className={styles.titulo}>Entrar</h1>
            <p className={styles.legenda}>Acesse sua conta.</p>
          </div>

          <form className={styles.formulario} onSubmit={enviar} noValidate>
            {login.isError && (
              <div className={styles.alerta} role="alert">
                <CircleAlert size={16} strokeWidth={1.5} />
                <span>{mensagemErroLogin(login.error)}</span>
              </div>
            )}

            <div className={styles.campo}>
              <label className={styles.rotulo} htmlFor="email">
                E-mail
              </label>
              <input
                ref={campoEmail}
                id="email"
                className={styles.entrada}
                type="email"
                autoComplete="username"
                inputMode="email"
                placeholder="voce@exemplo.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                aria-invalid={errosCampos.email ? true : undefined}
                aria-describedby={errosCampos.email ? 'email-erro' : undefined}
              />
              {errosCampos.email && (
                <span id="email-erro" className={styles.dica}>
                  {errosCampos.email}
                </span>
              )}
            </div>

            <div className={styles.campo}>
              <label className={styles.rotulo} htmlFor="senha">
                Senha
              </label>
              <input
                ref={campoSenha}
                id="senha"
                className={styles.entrada}
                type="password"
                autoComplete="current-password"
                placeholder="••••"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                aria-invalid={errosCampos.senha ? true : undefined}
                aria-describedby={errosCampos.senha ? 'senha-erro' : undefined}
              />
              {errosCampos.senha && (
                <span id="senha-erro" className={styles.dica}>
                  {errosCampos.senha}
                </span>
              )}
            </div>

            <button
              className={styles.botao}
              type="submit"
              disabled={login.isPending}
            >
              {login.isPending ? 'Entrando…' : 'Entrar'}
            </button>
          </form>
        </div>
      </div>
    </main>
  )
}
