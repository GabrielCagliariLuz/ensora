import type { KeyboardEvent } from 'react'
import styles from './CampoTexto.module.css'

type EventoTecla = KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>

interface CampoTextoProps {
  // Quem usa gera o id (useId) e o repassa: o formulário também o usa para
  // focar o primeiro campo com erro.
  id: string
  rotulo: string
  valor: string
  aoMudar: (valor: string) => void
  tipo?: 'text' | 'date' | 'password'
  multilinha?: boolean
  opcional?: boolean
  placeholder?: string
  erro?: string
  apoio?: string
  focoInicial?: boolean
  // Para campos com comportamento próprio (como o de horário).
  modoEntrada?: 'numeric'
  // Dica para o gerenciador de senhas (current-password, new-password).
  autoCompletar?: string
  maxLength?: number
  aoTeclar?: (evento: EventoTecla) => void
  aoSair?: () => void
}

export default function CampoTexto({
  id,
  rotulo,
  valor,
  aoMudar,
  tipo = 'text',
  multilinha = false,
  opcional = false,
  placeholder,
  erro,
  apoio,
  focoInicial = false,
  modoEntrada,
  autoCompletar,
  maxLength,
  aoTeclar,
  aoSair,
}: CampoTextoProps) {
  const idErro = `${id}-erro`

  // O <dialog> só aceita foco depois de aberto, então o foco inicial vem do
  // atributo autofocus, que o showModal() respeita. O autoFocus do React
  // chamaria focus() com o diálogo ainda fechado.
  const marcarFoco = (elemento: HTMLElement | null) => {
    if (focoInicial) elemento?.setAttribute('autofocus', '')
  }

  return (
    <div className={styles.campo}>
      <label className={styles.rotulo} htmlFor={id}>
        {rotulo}
        {opcional && <span className={styles.opcional}> (opcional)</span>}
      </label>
      {multilinha ? (
        <textarea
          ref={marcarFoco}
          id={id}
          className={`${styles.entrada} ${styles.areaTexto}`}
          rows={3}
          placeholder={placeholder}
          value={valor}
          onChange={(e) => aoMudar(e.target.value)}
          onKeyDown={aoTeclar}
          onBlur={aoSair}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? idErro : undefined}
        />
      ) : (
        <input
          ref={marcarFoco}
          id={id}
          className={styles.entrada}
          type={tipo}
          inputMode={modoEntrada}
          maxLength={maxLength}
          autoComplete={autoCompletar ?? 'off'}
          placeholder={placeholder}
          value={valor}
          onChange={(e) => aoMudar(e.target.value)}
          onKeyDown={aoTeclar}
          onBlur={aoSair}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? idErro : undefined}
        />
      )}
      {erro ? (
        <span id={idErro} className={styles.dica}>
          {erro}
        </span>
      ) : (
        apoio && <p className={styles.apoio}>{apoio}</p>
      )}
    </div>
  )
}
