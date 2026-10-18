import { LogOut } from 'lucide-react'
import CabecalhoPagina from '../../components/ui/CabecalhoPagina'
import { useSair } from '../auth/useSair'
import styles from './PaginaAjustes.module.css'

export default function PaginaAjustes() {
  const logout = useSair()

  return (
    <>
      <CabecalhoPagina titulo="Settings" legenda="Conta e preferências." />
      <section className={styles.secao}>
        <button
          className={styles.botaoSair}
          type="button"
          onClick={() => logout.mutate()}
          disabled={logout.isPending}
        >
          <LogOut size={16} strokeWidth={1.5} />
          {logout.isPending ? 'Saindo…' : 'Sair da conta'}
        </button>
      </section>
    </>
  )
}
