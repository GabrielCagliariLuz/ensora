import { Clock, MapPin, Pencil, Tag, Trash2, X } from 'lucide-react'
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import { createPortal } from 'react-dom'
import type { Entrega, Evento } from '../../api/tipos'
import { dataPorExtenso } from '../../lib/datas'
import { corDaCategoria, rotuloCategoria } from '../agenda/categorias'
import {
  formatarHora,
  formatarHorarioEvento,
  situacaoPrazo,
} from '../inicio/upcoming'
import type { Registro } from './agenda'
import styles from './PopoverRegistro.module.css'

const MARGEM = 8
const DISTANCIA = 6

interface PopoverRegistroProps {
  registro: Registro
  // Botão da etiqueta clicada: o popover abre junto dele.
  ancora: HTMLElement
  agora: Date
  aoAlternarEntrega: (entrega: Entrega) => void
  aoEditar: (registro: Registro) => void
  aoExcluir: (registro: Registro) => void
  aoFechar: () => void
}

function primeiraMaiuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

// Detalhes de um evento ou de uma entrega, num cartão ancorado à etiqueta.
export default function PopoverRegistro({
  registro,
  ancora,
  agora,
  aoAlternarEntrega,
  aoEditar,
  aoExcluir,
  aoFechar,
}: PopoverRegistroProps) {
  const cartao = useRef<HTMLDivElement>(null)
  const [posicao, setPosicao] = useState<{ top: number; left: number } | null>(
    null,
  )

  // Abaixo da etiqueta, alinhado à esquerda dela; vira para cima ou para o lado
  // quando falta espaço na janela.
  useLayoutEffect(() => {
    const elemento = cartao.current
    if (elemento === null) return

    const alvo = ancora.getBoundingClientRect()
    const { offsetWidth: largura, offsetHeight: altura } = elemento
    const left = Math.max(
      MARGEM,
      Math.min(alvo.left, window.innerWidth - largura - MARGEM),
    )
    const abaixo = alvo.bottom + DISTANCIA
    const top =
      abaixo + altura <= window.innerHeight - MARGEM
        ? abaixo
        : Math.max(MARGEM, alvo.top - DISTANCIA - altura)
    setPosicao({ top, left })
    elemento.focus()
  }, [ancora])

  useEffect(() => {
    function aoPressionar(evento: PointerEvent) {
      const alvo = evento.target as Node
      // Clicar na própria etiqueta é tratado por ela (abre ou fecha).
      if (cartao.current?.contains(alvo) || ancora.contains(alvo)) return
      aoFechar()
    }
    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key !== 'Escape') return
      evento.stopPropagation()
      aoFechar()
      ancora.focus()
    }
    // O popover não acompanha a rolagem nem o redimensionamento: fecha.
    document.addEventListener('pointerdown', aoPressionar)
    document.addEventListener('keydown', aoTeclar)
    window.addEventListener('scroll', aoFechar, true)
    window.addEventListener('resize', aoFechar)
    return () => {
      document.removeEventListener('pointerdown', aoPressionar)
      document.removeEventListener('keydown', aoTeclar)
      window.removeEventListener('scroll', aoFechar, true)
      window.removeEventListener('resize', aoFechar)
    }
  }, [ancora, aoFechar])

  const estilo: CSSProperties = posicao
    ? { top: posicao.top, left: posicao.left }
    : { top: 0, left: 0, visibility: 'hidden' }

  return createPortal(
    <div
      ref={cartao}
      className={styles.popover}
      style={estilo}
      role="dialog"
      aria-label="Detalhes do registro"
      tabIndex={-1}
    >
      <div className={styles.ferramentas}>
        <button
          className={styles.ferramenta}
          type="button"
          onClick={() => aoEditar(registro)}
          aria-label="Editar"
          title="Editar"
        >
          <Pencil size={16} strokeWidth={1.5} />
        </button>
        <button
          className={styles.ferramenta}
          type="button"
          onClick={() => aoExcluir(registro)}
          aria-label="Excluir"
          title="Excluir"
        >
          <Trash2 size={16} strokeWidth={1.5} />
        </button>
        <button
          className={styles.ferramenta}
          type="button"
          onClick={() => {
            aoFechar()
            ancora.focus()
          }}
          aria-label="Fechar"
          title="Fechar"
        >
          <X size={16} strokeWidth={1.5} />
        </button>
      </div>
      {registro.tipo === 'EVENTO' ? (
        <DetalhesEvento evento={registro.evento} />
      ) : (
        <DetalhesEntrega
          entrega={registro.entrega}
          agora={agora}
          aoAlternar={() => aoAlternarEntrega(registro.entrega)}
        />
      )}
    </div>,
    document.body,
  )
}

function DetalhesEvento({ evento }: { evento: Evento }) {
  const categoria = evento.categoria
  return (
    <>
      <div className={styles.cabecalho}>
        <span
          className={styles.ponto}
          style={{ background: corDaCategoria(categoria) }}
        />
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
  aoAlternar,
}: {
  entrega: Entrega
  agora: Date
  aoAlternar: () => void
}) {
  const situacao = situacaoPrazo(entrega, agora)
  const prazo = entrega.prazoHorario
    ? `${dataPorExtenso(entrega.prazoData)} · ${formatarHora(entrega.prazoHorario)}`
    : dataPorExtenso(entrega.prazoData)

  return (
    <>
      <div className={styles.cabecalho}>
        <span
          className={styles.ponto}
          style={{
            background: situacao.urgente ? 'var(--ember)' : 'var(--cat-neutra)',
          }}
        />
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
          situacao.urgente
            ? `${styles.situacao} ${styles.urgente}`
            : styles.situacao
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
