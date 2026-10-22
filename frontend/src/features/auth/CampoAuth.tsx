import styles from './PaginaLogin.module.css'

interface CampoAuthProps {
  id: string
  rotulo: string
  valor: string
  aoMudar: (valor: string) => void
  tipo?: 'text' | 'email' | 'password'
  autoCompletar?: string
  placeholder?: string
  erro?: string
  apoio?: string
}

// Campo das telas de acesso, no mesmo desenho do login (rótulo em mono, entrada
// alta e a mensagem de erro embaixo).
export default function CampoAuth({
  id,
  rotulo,
  valor,
  aoMudar,
  tipo = 'text',
  autoCompletar,
  placeholder,
  erro,
  apoio,
}: CampoAuthProps) {
  const idDica = `${id}-dica`

  return (
    <div className={styles.campo}>
      <label className={styles.rotulo} htmlFor={id}>
        {rotulo}
      </label>
      <input
        id={id}
        className={styles.entrada}
        type={tipo}
        inputMode={tipo === 'email' ? 'email' : undefined}
        autoComplete={autoCompletar}
        placeholder={placeholder}
        value={valor}
        onChange={(e) => aoMudar(e.target.value)}
        aria-invalid={erro ? true : undefined}
        aria-describedby={erro || apoio ? idDica : undefined}
      />
      {erro ? (
        <span id={idDica} className={styles.dica}>
          {erro}
        </span>
      ) : (
        apoio && (
          <span id={idDica} className={styles.apoio}>
            {apoio}
          </span>
        )
      )}
    </div>
  )
}
