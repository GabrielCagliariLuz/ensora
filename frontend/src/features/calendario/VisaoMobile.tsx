import { useCallback, useState } from 'react'
import type { ReactNode } from 'react'
import { Plus } from 'lucide-react'
import type { Entrega } from '../../api/tipos'
import CabecalhoPagina from '../../components/ui/CabecalhoPagina'
import { registrosDoDia } from './agenda'
import type { Registro, RegistrosDoDia } from './agenda'
import { dataDoRegistro, idDoRegistro } from './mover'
import MesMobile from './MesMobile'
import { hojeNaGrade, prazosDeHoje } from './prazosDeHoje'
import SecaoDiaSelecionado from './SecaoDiaSelecionado'
import SecaoPrazosHoje from './SecaoPrazosHoje'
import SheetDetalhesRegistro from './SheetDetalhesRegistro'
import styles from './VisaoMobile.module.css'

interface VisaoMobileProps {
  mes: string
  hoje: string
  agora: Date
  dias: Map<string, RegistrosDoDia>
  carregando: boolean
  selecionado: string
  // Aviso de erro (conclusão, movimento), já montado pela página.
  aviso: ReactNode
  aoAnterior: () => void
  aoProximo: () => void
  aoEscolherDia: (dia: string) => void
  aoAlternarEntrega: (entrega: Entrega) => void
  aoEditar: (registro: Registro) => void
  aoExcluir: (registro: Registro) => void
  aoNovoRegistro: () => void
}

// O registro com os detalhes abertos, por identidade (dia, tipo e id): concluir
// troca o objeto, e o sheet acompanha.
interface Aberto {
  dia: string
  tipo: Registro['tipo']
  id: number
}

// Visão do Calendar abaixo de 768px: cartão do mês com pontos, "Prazos de
// Hoje", a lista do dia selecionado e o botão de novo registro.
export default function VisaoMobile({
  mes,
  hoje,
  agora,
  dias,
  carregando,
  selecionado,
  aviso,
  aoAnterior,
  aoProximo,
  aoEscolherDia,
  aoAlternarEntrega,
  aoEditar,
  aoExcluir,
  aoNovoRegistro,
}: VisaoMobileProps) {
  const [aberto, setAberto] = useState<Aberto | null>(null)
  const fechar = useCallback(() => setAberto(null), [])

  const registroAberto =
    aberto === null
      ? null
      : (registrosDoDia(dias.get(aberto.dia)).find(
          (registro) =>
            registro.tipo === aberto.tipo && idDoRegistro(registro) === aberto.id,
        ) ?? null)

  // Com hoje selecionado, as entregas do dia já estão em "Prazos de Hoje": a
  // lista do dia mostra só os eventos, e some se não houver nenhum.
  const registrosDoSelecionado = registrosDoDia(dias.get(selecionado))
  const ehHoje = selecionado === hoje
  const registrosDaLista = ehHoje
    ? registrosDoSelecionado.filter((registro) => registro.tipo === 'EVENTO')
    : registrosDoSelecionado

  function abrir(registro: Registro) {
    setAberto({
      dia: dataDoRegistro(registro),
      tipo: registro.tipo,
      id: idDoRegistro(registro),
    })
  }

  return (
    <div className={styles.pagina}>
      <CabecalhoPagina titulo="Calendar" />

      {aviso}

      <MesMobile
        mes={mes}
        hoje={hoje}
        agora={agora}
        selecionado={selecionado}
        dias={dias}
        carregando={carregando}
        aoAnterior={aoAnterior}
        aoProximo={aoProximo}
        aoEscolherDia={aoEscolherDia}
      />

      {hojeNaGrade(mes, hoje) && (
        <SecaoPrazosHoje
          entregas={prazosDeHoje(dias, mes, hoje)}
          hoje={hoje}
          agora={agora}
          aoAbrir={abrir}
          aoAlternarEntrega={aoAlternarEntrega}
        />
      )}

      {(!ehHoje || registrosDaLista.length > 0) && (
        <SecaoDiaSelecionado
          dia={selecionado}
          titulo={ehHoje ? 'Eventos de Hoje' : undefined}
          registros={registrosDaLista}
          agora={agora}
          aoAbrir={abrir}
          aoAlternarEntrega={aoAlternarEntrega}
        />
      )}

      <button className={styles.novo} type="button" onClick={aoNovoRegistro}>
        <Plus size={16} strokeWidth={1.5} />
        Novo Registro
      </button>

      <SheetDetalhesRegistro
        registro={registroAberto}
        agora={agora}
        aoFechar={fechar}
        aoAlternarEntrega={aoAlternarEntrega}
        // Os diálogos ficam lado a lado: fecha os detalhes antes de abrir o
        // sheet de edição ou o modal de exclusão.
        aoEditar={(registro) => {
          fechar()
          aoEditar(registro)
        }}
        aoExcluir={(registro) => {
          fechar()
          aoExcluir(registro)
        }}
      />
    </div>
  )
}
