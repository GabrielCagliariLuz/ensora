import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import type { Entrega } from '../../api/tipos'
import BlocoAviso from '../../components/ui/BlocoAviso'
import { mesDoDia } from './grade'
import { useAgora } from '../../lib/useAgora'
import { useEhMobile } from '../../lib/useEhMobile'
import { useHoje } from '../../lib/useHoje'
import ModalExcluirRegistro from '../agenda/ModalExcluirRegistro'
import SheetEditarRegistro from '../agenda/SheetEditarRegistro'
import SheetNovoRegistro from '../agenda/SheetNovoRegistro'
import { mensagemErroMovimento } from '../agenda/regras'
import { mensagemErroConclusao } from '../inicio/regras'
import { useConcluirEntrega } from '../inicio/useConcluirEntrega'
import { agruparPorDia } from './agenda'
import { pedidoDaBusca } from './diaDaUrl'
import { useDiaDestacado } from './useDiaDestacado'
import type { Registro } from './agenda'
import CabecalhoCalendario from './CabecalhoCalendario'
import GradeMes from './GradeMes'
import { intervaloDaGrade } from './grade'
import { useCalendario, usePrefetchMesesVizinhos } from './useCalendario'
import { useDiaSelecionado } from './useDiaSelecionado'
import { useMesVisivel } from './useMesVisivel'
import { useMoverRegistro } from './useMoverRegistro'
import VisaoMobile from './VisaoMobile'
import styles from './PaginaCalendario.module.css'

export default function PaginaCalendario() {
  const hoje = useHoje()
  const agora = useAgora()
  const ehMobile = useEhMobile()
  // Quem chega da Home traz o dia (e o registro) na URL: o Calendar abre nesse
  // mês e dia, com os detalhes do registro abertos. Só vale na chegada; depois,
  // navegar pelos meses é livre.
  const [busca] = useSearchParams()
  const [pedido] = useState(() => pedidoDaBusca(busca))
  const diaDaUrl = pedido?.dia ?? null
  const abertoInicial =
    pedido?.registro != null ? { dia: pedido.dia, ...pedido.registro } : null
  const diaDestacado = useDiaDestacado(diaDaUrl)
  const { mes, anterior, proximo, irParaHoje, irParaMes } = useMesVisivel(
    hoje,
    diaDaUrl,
  )
  const { dia: selecionado, escolher } = useDiaSelecionado(mes, hoje, diaDaUrl)
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

  // Tocou num dia do cartão do mês (mobile). Um dia de outro mês, na borda da
  // grade, leva o mês junto.
  function escolherDia(dia: string) {
    if (mesDoDia(dia) !== mes) irParaMes(mesDoDia(dia))
    escolher(dia)
  }

  // Arrastou uma etiqueta para outro dia.
  function moverPara(registro: Registro, para: string) {
    setAviso(null)
    mover.mutate({ registro, para })
  }

  const blocoAviso = aviso && (
    <div className={styles.aviso}>
      <BlocoAviso tom="erro">{aviso}</BlocoAviso>
    </div>
  )

  const estadoSemDados = calendario.isError ? (
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
  )

  return (
    <>
      {ehMobile ? (
        dias !== null ? (
          <VisaoMobile
            mes={mes}
            hoje={hoje}
            agora={agora}
            dias={dias}
            carregando={calendario.isPlaceholderData}
            selecionado={selecionado}
            abertoInicial={abertoInicial}
            aviso={blocoAviso}
            aoAnterior={anterior}
            aoProximo={proximo}
            aoEscolherDia={escolherDia}
            aoAlternarEntrega={alternar}
            aoEditar={setEditando}
            aoExcluir={setExcluindo}
            aoNovoRegistro={() => setNovo(true)}
          />
        ) : (
          estadoSemDados
        )
      ) : (
        <>
          <CabecalhoCalendario
            mes={mes}
            aoAnterior={anterior}
            aoProximo={proximo}
            aoHoje={irParaHoje}
            aoNovoRegistro={() => setNovo(true)}
          />
          {blocoAviso}
          {dias !== null ? (
            <GradeMes
              mes={mes}
              hoje={hoje}
              agora={agora}
              dias={dias}
              diaDestacado={diaDestacado}
              abertoInicial={abertoInicial}
              carregando={calendario.isPlaceholderData}
              aoAlternarEntrega={alternar}
              aoEditar={setEditando}
              aoExcluir={setExcluindo}
              aoMover={moverPara}
            />
          ) : (
            estadoSemDados
          )}
        </>
      )}

      <SheetNovoRegistro
        aberto={novo}
        aoFechar={() => setNovo(false)}
        dataInicial={ehMobile ? selecionado : undefined}
      />
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
