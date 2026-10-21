import { Clock, MapPin, Tag } from 'lucide-react'
import type { Entrega, Evento } from '../../api/tipos'
import { dataPorExtenso } from '../../lib/datas'
import { rotuloCategoria } from '../agenda/categorias'
import {
  formatarHora,
  formatarHorarioEvento,
  situacaoPrazo,
} from '../inicio/upcoming'
import type { Registro } from './agenda'
import { corDoRegistro, entregaUrgente } from './cores'
import styles from './DetalhesRegistro.module.css'

interface DetalhesRegistroProps {
  registro: Registro
  agora: Date
  // Concluir ou desfazer a entrega (o botão só existe para entregas).
  aoAlternarEntrega: (entrega: Entrega) => void
  // Deixa espaço à direita do título para os ícones de ferramentas do popover.
  reservarFerramentas?: boolean
}

function primeiraMaiuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

// O conteúdo de detalhes de um evento ou de uma entrega, igual no popover do
// desktop e no sheet do mobile.
export default function DetalhesRegistro({
  registro,
  agora,
  aoAlternarEntrega,
  reservarFerramentas = false,
}: DetalhesRegistroProps) {
  const cabecalho = reservarFerramentas
    ? `${styles.cabecalho} ${styles.comFerramentas}`
    : styles.cabecalho

  return (
    <div className={styles.detalhes}>
      {registro.tipo === 'EVENTO' ? (
        <DetalhesEvento
          evento={registro.evento}
          cor={corDoRegistro(registro, agora)}
          classeCabecalho={cabecalho}
        />
      ) : (
        <DetalhesEntrega
          entrega={registro.entrega}
          agora={agora}
          cor={corDoRegistro(registro, agora)}
          classeCabecalho={cabecalho}
          aoAlternar={() => aoAlternarEntrega(registro.entrega)}
        />
      )}
    </div>
  )
}

function DetalhesEvento({
  evento,
  cor,
  classeCabecalho,
}: {
  evento: Evento
  cor: string
  classeCabecalho: string
}) {
  const categoria = evento.categoria
  return (
    <>
      <div className={classeCabecalho}>
        <span className={styles.ponto} style={{ background: cor }} />
        <h2 className={styles.titulo}>{evento.titulo}</h2>
      </div>
      <ul className={styles.linhas}>
        <li>
          <Clock size={14} strokeWidth={1.5} />
          <span>
            {dataPorExtenso(evento.data)} ·{' '}
            {formatarHorarioEvento(evento.inicio, evento.termino)}
          </span>
        </li>
        {evento.local && (
          <li>
            <MapPin size={14} strokeWidth={1.5} />
            <span>{evento.local}</span>
          </li>
        )}
        {categoria && (
          <li>
            <Tag size={14} strokeWidth={1.5} />
            <span>{rotuloCategoria(categoria)}</span>
          </li>
        )}
      </ul>
    </>
  )
}

function DetalhesEntrega({
  entrega,
  agora,
  cor,
  classeCabecalho,
  aoAlternar,
}: {
  entrega: Entrega
  agora: Date
  cor: string
  classeCabecalho: string
  aoAlternar: () => void
}) {
  const situacao = situacaoPrazo(entrega, agora)
  const urgente = entregaUrgente(entrega, agora)
  const prazo = entrega.prazoHorario
    ? `${dataPorExtenso(entrega.prazoData)} · ${formatarHora(entrega.prazoHorario)}`
    : dataPorExtenso(entrega.prazoData)

  return (
    <>
      <div className={classeCabecalho}>
        <span className={styles.ponto} style={{ background: cor }} />
        <h2
          className={
            entrega.concluida
              ? `${styles.titulo} ${styles.concluida}`
              : styles.titulo
          }
        >
          {entrega.titulo}
        </h2>
      </div>
      <ul className={styles.linhas}>
        <li>
          <Clock size={14} strokeWidth={1.5} />
          <span>Prazo: {prazo}</span>
        </li>
      </ul>
      <p
        className={
          urgente ? `${styles.situacao} ${styles.urgente}` : styles.situacao
        }
      >
        {primeiraMaiuscula(situacao.texto)}
      </p>
      {entrega.nota && <p className={styles.nota}>{entrega.nota}</p>}
      <button
        className={
          entrega.concluida
            ? `${styles.acao} ${styles.desfazer}`
            : styles.acao
        }
        type="button"
        onClick={aoAlternar}
      >
        {entrega.concluida ? 'Desfazer' : 'Concluir'}
      </button>
    </>
  )
}
