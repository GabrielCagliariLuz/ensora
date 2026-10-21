import { useMemo, useState } from 'react'
import type { Entrega } from '../../api/tipos'
import BlocoAviso from '../../components/ui/BlocoAviso'
import { useAgora } from '../../lib/useAgora'
import { useHoje } from '../../lib/useHoje'
import ModalExcluirRegistro from '../agenda/ModalExcluirRegistro'
import SheetEditarRegistro from '../agenda/SheetEditarRegistro'
import SheetNovoRegistro from '../agenda/SheetNovoRegistro'
import { mensagemErroMovimento } from '../agenda/regras'
import { mensagemErroConclusao } from '../inicio/regras'
import { useConcluirEntrega } from '../inicio/useConcluirEntrega'
import { agruparPorDia } from './agenda'
import type { Registro } from './agenda'
import CabecalhoCalendario from './CabecalhoCalendario'
import GradeMes from './GradeMes'
import { intervaloDaGrade } from './grade'
import { useCalendario, usePrefetchMesesVizinhos } from './useCalendario'
import { useMesVisivel } from './useMesVisivel'
import { useMoverRegistro } from './useMoverRegistro'
import styles from './PaginaCalendario.module.css'

export default function PaginaCalendario() {
  const hoje = useHoje()
  const agora = useAgora()
  const { mes, anterior, proximo, irParaHoje } = useMesVisivel(hoje)
  const { de, ate } = intervaloDaGrade(mes)
  const calendario = useCalendario(de, ate)
  usePrefetchMesesVizinhos(mes)

  const [aviso, setAviso] = useState<string | null>(null)
  const [novo, setNovo] = useState(false)
  const [editando, setEditando] = useState<Registro | null>(null)
  const [excluindo, setExcluindo] = useState<Registro | null>(null)

  const concluir = useConcluirEntrega((erro, conclusao) =>
    setAviso(mensagemErroConclusao(erro, conclusao.titulo)),
  )

  const mover = useMoverRegistro((erro, movimento) =>
    setAviso(
      mensagemErroMovimento(
        erro,
        movimento.registro.tipo === 'EVENTO'
          ? movimento.registro.evento.titulo
          : movimento.registro.entrega.titulo,
      ),
    ),
  )

  const dias = useMemo(
    () => (calendario.data ? agruparPorDia(calendario.data) : null),
    [calendario.data],
  )

  // O alvo é o contrário do que a etiqueta mostra agora, já com a conclusão
  // otimista aplicada. O aviso anterior some no próximo clique.
  function alternar(entrega: Entrega) {
    setAviso(null)
    concluir.mutate({
      id: entrega.id,
      titulo: entrega.titulo,
      concluida: !entrega.concluida,
      data: hoje,
    })
  }

  // Arrastou uma etiqueta para outro dia.
  function moverPara(registro: Registro, para: string) {
    setAviso(null)
    mover.mutate({ registro, para })
  }

  return (
    <>
      <CabecalhoCalendario
        mes={mes}
        aoAnterior={anterior}
        aoProximo={proximo}
        aoHoje={irParaHoje}
        aoNovoRegistro={() => setNovo(true)}
      />

      {aviso && (
        <div className={styles.aviso}>
          <BlocoAviso tom="erro">{aviso}</BlocoAviso>
        </div>
      )}

      {dias !== null ? (
        <GradeMes
          mes={mes}
          hoje={hoje}
          agora={agora}
          dias={dias}
          carregando={calendario.isPlaceholderData}
          aoAlternarEntrega={alternar}
          aoEditar={setEditando}
          aoExcluir={setExcluindo}
          aoMover={moverPara}
        />
      ) : calendario.isError ? (
        <div className={styles.estado}>
          <p>Não foi possível carregar o calendário.</p>
          <button
            className={styles.botao}
            type="button"
            onClick={() => void calendario.refetch()}
          >
            Tentar de novo
          </button>
        </div>
      ) : (
        <p className={styles.estado}>Carregando…</p>
      )}

      <SheetNovoRegistro aberto={novo} aoFechar={() => setNovo(false)} />
      <SheetEditarRegistro
        registro={editando}
        aoFechar={() => setEditando(null)}
      />
      <ModalExcluirRegistro
        registro={excluindo}
        aoFechar={() => setExcluindo(null)}
      />
    </>
  )
}
