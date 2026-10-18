import { NavLink } from 'react-router'
import { itensNavegacao } from './itensNavegacao'
import styles from './BottomNav.module.css'

export default function BottomNav() {
  return (
    <nav className={styles.nav} aria-label="Navegação principal">
      {itensNavegacao.map(({ rotulo, caminho, Icone }) => (
        <NavLink
          key={caminho}
          to={caminho}
          end={caminho === '/'}
          aria-label={rotulo}
          className={({ isActive }) =>
            isActive ? `${styles.item} ${styles.ativo}` : styles.item
          }
        >
          <Icone size={24} strokeWidth={1.5} />
        </NavLink>
      ))}
    </nav>
  )
}
