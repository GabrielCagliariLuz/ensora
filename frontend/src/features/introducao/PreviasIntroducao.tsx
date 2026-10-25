import type { ReactNode } from 'react'
import { Calendar, Flag } from 'lucide-react'
import type { Entrega, Evento } from '../../api/tipos'
import AnelEnsora from '../../components/ui/AnelEnsora'
import { somarDias } from '../../lib/datas'
import { useHoje } from '../../lib/useHoje'
import ItemEntrega from '../inicio/ItemEntrega'
import ItemEvento from '../inicio/ItemEvento'
import ItemHabito from '../inicio/ItemHabito'
import RitmoDoMes from '../insights/RitmoDoMes'
import type { BarraSemanal } from '../insights/ritmo'
import type { IdPasso } from './regras'
import styles from './PreviasIntroducao.module.css'

// Os exemplos do tutorial usam os mesmos componentes da Home e dos Insights,
// com dados de mentira: o que a pessoa vê aqui é o que vai ver depois. Quem os
// usa os esconde de leitores de tela e os deixa inertes (sem clique nem foco),
// porque os itens de entrega e evento são links para o Calendar.

export default function PreviaDoPasso({ id }: { id: IdPasso }) {
  switch (id) {
    case 'BOAS_VINDAS':
      return <PreviaBoasVindas />
    case 'HABITOS':
      return <PreviaHabitos />
    case 'ENTREGAS_E_EVENTOS':
      return <PreviaEntregasEEventos />
    case 'INSIGHTS':
      return <PreviaInsights />
  }
}

function PreviaBoasVindas() {
  return (
    <ul className={styles.conceitos}>
      <Conceito
        icone={<AnelEnsora className={styles.anel} />}
        rotulo="Hábito"
        texto="Repete e você marca"
      />
      <Conceito
        icone={<Flag size={20} strokeWidth={1.5} />}
        rotulo="Entrega"
        texto="Tem prazo e você conclui"
      />
      <Conceito
        icone={<Calendar size={20} strokeWidth={1.5} />}
        rotulo="Evento"
        texto="Tem dia e hora"
      />
    </ul>
  )
}

interface ConceitoProps {
  icone: ReactNode
  rotulo: string
  texto: string
}

function Conceito({ icone, rotulo, texto }: ConceitoProps) {
  return (
    <li className={styles.conceito}>
      <span className={styles.icone}>{icone}</span>
      <span className={styles.rotulo}>{rotulo}</span>
      <span className={styles.texto}>{texto}</span>
    </li>
  )
}

// Um hábito diário por fazer e um semanal já marcado hoje, com o contador.
function PreviaHabitos() {
  return (
    <ul className={styles.habitos}>
      <li>
        <ItemHabito nome="Ler" feito={false} />
      </li>
      <li>
        <ItemHabito
          nome="Academia"
          feito
          progresso={{ feitos: 1, meta: 3 }}
        />
      </li>
    </ul>
  )
}

function semAcao() {}

function PreviaEntregasEEventos() {
  const hoje = useHoje()
  // O relógio do exemplo é fixo (14h30 de hoje): a entrega das 17h30 aparece
  // sempre como "hoje", com o alerta de prazo próximo, seja qual for a hora real.
  const agora = new Date(`${hoje}T14:30:00`)

  const entrega: Entrega = {
    id: 0,
    titulo: 'Entregar o trabalho',
    prazoData: hoje,
    prazoHorario: '17:30:00',
    concluida: false,
  }
  const evento: Evento = {
    id: 0,
    titulo: 'Consulta',
    data: somarDias(hoje, 1),
    inicio: '09:00:00',
    categoria: 'SAUDE',
  }

  return (
    <div className={styles.cartoes}>
      <div className={styles.cartao}>
        <ItemEntrega entrega={entrega} agora={agora} aoAlternar={semAcao} />
      </div>
      <div className={styles.cartao}>
        <ItemEvento evento={evento} hoje={hoje} agora={agora} />
      </div>
    </div>
  )
}

function PreviaInsights() {
  const hoje = useHoje()
  const mes = hoje.slice(0, 7)
  // Quatro semanas fixas de sete dias: valem para qualquer mês, até fevereiro.
  const barras: BarraSemanal[] = [
    { rotulo: 'S1', de: `${mes}-01`, ate: `${mes}-07`, ritmo: 82, atual: false },
    { rotulo: 'S2', de: `${mes}-08`, ate: `${mes}-14`, ritmo: 88, atual: false },
    { rotulo: 'S3', de: `${mes}-15`, ate: `${mes}-21`, ritmo: 85, atual: false },
    { rotulo: 'S4', de: `${mes}-22`, ate: `${mes}-28`, ritmo: 86, atual: false },
  ]

  return <RitmoDoMes mes={mes} ritmoMedio={85} barras={barras} />
}
