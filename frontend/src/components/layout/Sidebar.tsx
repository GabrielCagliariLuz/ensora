import { PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { NavLink } from 'react-router'
import LogoEnsora from '../ui/LogoEnsora'
import { itemSettings, itensPrincipais } from './itensNavegacao'
import type { ItemNavegacao } from './itensNavegacao'
import styles from './Sidebar.module.css'

interface ItemSidebarProps {
  item: ItemNavegacao
  colapsada: boolean
  rodape?: boolean
}

function ItemSidebar({
  item: { rotulo, caminho, Icone },
  colapsada,
  rodape = false,
}: ItemSidebarProps) {
  return (
    <NavLink
      to={caminho}
      end={caminho === '/'}
      title={colapsada ? rotulo : undefined}
      className={({ isActive }) =>
        [styles.item, isActive && styles.ativo, rodape && styles.rodape]
          .filter(Boolean)
          .join(' ')
      }
    >
      <Icone size={24} strokeWidth={1.5} />
      <span className={colapsada ? styles.rotuloOculto : styles.rotulo}>
        {rotulo}
      </span>
    </NavLink>
  )
}

interface SidebarProps {
  colapsada: boolean
  aoAlternar: () => void
}

export default function Sidebar({ colapsada, aoAlternar }: SidebarProps) {
  const rotuloBotao = colapsada ? 'Expandir menu' : 'Recolher menu'
  const IconeBotao = colapsada ? PanelLeftOpen : PanelLeftClose

  return (
    <>
      <aside className={styles.sidebar}>
        <div className={styles.marca}>
          <LogoEnsora somenteAnel={colapsada} />
        </div>
        <nav className={styles.nav} aria-label="Navegação principal">
          {itensPrincipais.map((item) => (
            <ItemSidebar key={item.caminho} item={item} colapsada={colapsada} />
          ))}
          <ItemSidebar item={itemSettings} colapsada={colapsada} rodape />
        </nav>
      </aside>
      <button
        type="button"
        className={styles.alternar}
        onClick={aoAlternar}
        aria-expanded={!colapsada}
        aria-label={rotuloBotao}
        title={rotuloBotao}
      >
        <IconeBotao size={16} strokeWidth={1.5} />
      </button>
    </>
  )
}
