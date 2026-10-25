import { useId, useState } from 'react'
import CaixaDialogo from '../../components/ui/CaixaDialogo'
import PreviaDoPasso from './PreviasIntroducao'
import {
  ehPrimeiroPasso,
  ehUltimoPasso,
  PASSOS,
  passoAnterior,
  proximoPasso,
  rotuloAvancar,
  rotuloContagem,
} from './regras'
import styles from './ModalIntroducao.module.css'

interface ModalIntroducaoProps {
  aberto: boolean
  // Pular, Começar, Esc e clique fora: todos encerram o tutorial.
  aoEncerrar: () => void
}

// O foco inicial vem do atributo autofocus, que o showModal() respeita; o
// autoFocus do React chamaria focus() com o diálogo ainda fechado (o mesmo
// motivo do FormularioHabito). Função fora do componente: o React só a chama
// quando o botão entra e sai da tela, e não a cada passo.
function focoInicial(elemento: HTMLButtonElement | null) {
  elemento?.setAttribute('autofocus', '')
}

// No mobile sobe colado embaixo; no desktop vira um cartão centralizado. Não
// fecha arrastando, para um gesto sem querer não encerrar o tutorial.
export default function ModalIntroducao({
  aberto,
  aoEncerrar,
}: ModalIntroducaoProps) {
  const idTitulo = useId()

  return (
    <CaixaDialogo
      aberto={aberto}
      aoFechar={aoEncerrar}
      idTitulo={idTitulo}
      className={styles.modal}
    >
      <ConteudoIntroducao idTitulo={idTitulo} aoEncerrar={aoEncerrar} />
    </CaixaDialogo>
  )
}

// Só existe com o modal aberto (a CaixaDialogo monta os filhos ao abrir), então
// o tutorial sempre começa no primeiro passo.
function ConteudoIntroducao({
  idTitulo,
  aoEncerrar,
}: {
  idTitulo: string
  aoEncerrar: () => void
}) {
  const [indice, setIndice] = useState(0)
  const passo = PASSOS[indice]

  return (
    <div className={styles.corpo}>
      <div className={styles.topo}>
        <span className={styles.contagem}>{rotuloContagem(indice)}</span>
        <button className={styles.pular} type="button" onClick={aoEncerrar}>
          Pular introdução
        </button>
      </div>

      {/* Exemplo do passo: decorativo e inerte, não entra na leitura de tela. */}
      <div className={styles.palco} inert aria-hidden="true">
        <PreviaDoPasso id={passo.id} />
      </div>

      <div aria-live="polite">
        <h2 id={idTitulo} className={styles.titulo}>
          {passo.titulo}
        </h2>
        <p className={styles.texto}>{passo.texto}</p>
      </div>

      <div className={styles.rodape}>
        <ol className={styles.pontos} aria-hidden="true">
          {PASSOS.map((outro, i) => (
            <li
              key={outro.id}
              className={
                i === indice ? `${styles.ponto} ${styles.atual}` : styles.ponto
              }
            />
          ))}
        </ol>
        <div className={styles.botoes}>
          {!ehPrimeiroPasso(indice) && (
            <button
              className={styles.voltar}
              type="button"
              onClick={() => setIndice(passoAnterior(indice))}
            >
              Voltar
            </button>
          )}
          <button
            ref={focoInicial}
            className={styles.avancar}
            type="button"
            onClick={() =>
              ehUltimoPasso(indice)
                ? aoEncerrar()
                : setIndice(proximoPasso(indice))
            }
          >
            {rotuloAvancar(indice)}
          </button>
        </div>
      </div>
    </div>
  )
}
