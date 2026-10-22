import { useMemo } from 'react'
import { gruposDeFuso, todosOsFusos } from '../../lib/fusos'
import styles from './SeletorFuso.module.css'

interface SeletorFusoProps {
  id: string
  // O fuso já salvo na conta: entra na lista mesmo que o navegador não o conheça.
  atual: string
  valor: string
  aoMudar: (fuso: string) => void
}

// Lista de fusos: os do Brasil primeiro, depois todos os outros em ordem
// alfabética.
export default function SeletorFuso({
  id,
  atual,
  valor,
  aoMudar,
}: SeletorFusoProps) {
  const grupos = useMemo(() => gruposDeFuso(atual, todosOsFusos()), [atual])

  return (
    <div className={styles.campo}>
      <label className={styles.rotulo} htmlFor={id}>
        Fuso horário
      </label>
      <select
        id={id}
        className={styles.entrada}
        value={valor}
        onChange={(e) => aoMudar(e.target.value)}
      >
        <optgroup label="Brasil">
          {grupos.brasil.map((opcao) => (
            <option key={opcao.valor} value={opcao.valor}>
              {opcao.rotulo}
            </option>
          ))}
        </optgroup>
        {grupos.outros.length > 0 && (
          <optgroup label="Outros fusos">
            {grupos.outros.map((opcao) => (
              <option key={opcao.valor} value={opcao.valor}>
                {opcao.rotulo}
              </option>
            ))}
          </optgroup>
        )}
      </select>
      <p className={styles.apoio}>
        Define o &quot;hoje&quot; e o dia em que cada hábito começou nos Insights.
      </p>
    </div>
  )
}
