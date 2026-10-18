import { WifiOff } from 'lucide-react'
import { useRevalidator, useRouteError } from 'react-router'
import { ErroApi } from '../../api/erro'
import styles from './TelaErroConexao.module.css'

export default function TelaErroConexao() {
  const erro = useRouteError()
  const { revalidate, state } = useRevalidator()
  const tentando = state === 'loading'
  const semConexao = erro instanceof ErroApi && erro.status === 0

  return (
    <div className={styles.tela}>
      <div className={styles.icone}>
        <WifiOff size={22} strokeWidth={1.5} />
      </div>
      <h1 className={styles.titulo}>
        {semConexao ? 'Sem conexão com o servidor' : 'Algo deu errado'}
      </h1>
      <p className={styles.texto}>
        {semConexao
          ? 'Não deu para verificar sua sessão. Confira a internet e tente de novo.'
          : 'O servidor não respondeu como esperado. Tente de novo em instantes.'}
      </p>
      <button
        className={styles.botao}
        type="button"
        onClick={() => void revalidate()}
        disabled={tentando}
      >
        {tentando ? 'Tentando…' : 'Tentar de novo'}
      </button>
    </div>
  )
}
