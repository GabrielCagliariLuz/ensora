import { Outlet } from 'react-router'
import BottomNav from './BottomNav'
import CabecalhoMobile from './CabecalhoMobile'
import Sidebar from './Sidebar'
import { useSidebarColapsada } from './useSidebarColapsada'
import styles from './AppShell.module.css'

export default function AppShell() {
  const [colapsada, alternar] = useSidebarColapsada()

  return (
    <div
      className={
        colapsada ? `${styles.shell} ${styles.colapsada}` : styles.shell
      }
    >
      <Sidebar colapsada={colapsada} aoAlternar={alternar} />
      <main className={styles.conteudo}>
        <CabecalhoMobile />
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
