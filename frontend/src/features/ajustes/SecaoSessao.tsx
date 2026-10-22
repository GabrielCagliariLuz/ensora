import { LogOut } from 'lucide-react'
import { useSair } from '../auth/useSair'
import CartaoAjuste from './CartaoAjuste'
import styles from './CartaoAjuste.module.css'

export default function SecaoSessao() {
  const logout = useSair()

  return (
    <CartaoAjuste titulo="Sessão">
      <div className={styles.acoes}>
        <button
          className={styles.botao}
          type="button"
          onClick={() => logout.mutate()}
          disabled={logout.isPending}
        >
          <LogOut size={16} strokeWidth={1.5} />
          {logout.isPending ? 'Saindo…' : 'Sair da conta'}
        </button>
      </div>
    </CartaoAjuste>
  )
}
