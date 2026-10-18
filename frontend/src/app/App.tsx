import { Flame } from "lucide-react";
import styles from "./App.module.css";

export default function App() {
  return (
    <main className={styles.pagina}>
      <h1 className={styles.titulo}>Ensora</h1>
      <p className={styles.legenda}>Base visual carregada.</p>
      <div className={styles.card}>
        <span className={styles.rotulo}>
          <Flame size={14} strokeWidth={1.5} />
          SEQUÊNCIA
        </span>
        <span className={styles.numero}>12</span>
      </div>
    </main>
  );
}
