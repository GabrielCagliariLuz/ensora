import { useCallback, useState } from 'react'
import type { Entrega } from '../../api/tipos'
import { registrosDoDia } from './agenda'
import type { Registro, RegistrosDoDia } from './agenda'
import CelulaDia from './CelulaDia'
import { gradeDoMes } from './grade'
import { dataDoRegistro, idDoRegistro } from './mover'
import styles from './GradeMes.module.css'
import PopoverRegistro from './PopoverRegistro'

const DIAS_DA_SEMANA = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB']

interface GradeMesProps {
  // Mês visível ('AAAA-MM').
  mes: string
  hoje: string
  agora: Date
  // Registros por dia (AAAA-MM-DD), de agruparPorDia.
  dias: Map<string, RegistrosDoDia>
  // Os dados na tela são do mês anterior: a grade esmaece até os novos chegarem.
  carregando?: boolean
  aoAlternarEntrega: (entrega: Entrega) => void
  aoEditar: (registro: Registro) => void
  aoExcluir: (registro: Registro) => void
  // Soltou o registro em outro dia.
  aoMover: (registro: Registro, para: string) => void
}

// O registro aberto fica guardado por identidade (dia, tipo e id), não pelo
// objeto: concluir uma entrega troca o objeto, e o popover acompanha.
interface Aberto {
  dia: string
  tipo: Registro['tipo']
  id: number
  ancora: HTMLElement
}

// Grade mensal de domingo a sábado, com dias dos meses vizinhos.
export default function GradeMes({
  mes,
  hoje,
  agora,
  dias,
  carregando = false,
  aoAlternarEntrega,
  aoEditar,
  aoExcluir,
  aoMover,
}: GradeMesProps) {
  const [aberto, setAberto] = useState<Aberto | null>(null)
  // Registro em arraste e o dia sob o cursor.
  const [arrastando, setArrastando] = useState<Registro | null>(null)
  const [alvo, setAlvo] = useState<string | null>(null)
  const fechar = useCallback(() => setAberto(null), [])

  const registroAberto =
    aberto === null
      ? undefined
      : registrosDoDia(dias.get(aberto.dia)).find(
          (registro) =>
            registro.tipo === aberto.tipo && idDoRegistro(registro) === aberto.id,
        )

  function comecarArrasto(registro: Registro) {
    setAberto(null)
    setArrastando(registro)
  }

  function terminarArrasto() {
    setArrastando(null)
    setAlvo(null)
  }

  function soltar(dia: string) {
    if (arrastando !== null) aoMover(arrastando, dia)
    terminarArrasto()
  }

  // Clicar de novo na etiqueta aberta fecha o popover.
  function abrir(dia: string, registro: Registro, ancora: HTMLElement) {
    const mesmo =
      aberto?.ancora === ancora ||
      (aberto?.dia === dia &&
        aberto.tipo === registro.tipo &&
        aberto.id === idDoRegistro(registro))
    setAberto(
      mesmo ? null : { dia, tipo: registro.tipo, id: idDoRegistro(registro), ancora },
    )
  }

  return (
    <>
      <div className={styles.cartao}>
        <div className={styles.semana} aria-hidden="true">
          {DIAS_DA_SEMANA.map((dia) => (
            <span key={dia} className={styles.diaDaSemana}>
              {dia}
            </span>
          ))}
        </div>
        {/* A chave refaz a grade a cada mês, e ela surge com um fade. */}
        <div
          key={mes}
          className={
            carregando ? `${styles.dias} ${styles.carregando}` : styles.dias
          }
        >
          {gradeDoMes(mes).map((dia) => (
            <CelulaDia
              key={dia}
              dia={dia}
              mesVisivel={mes}
              hoje={hoje}
              registros={registrosDoDia(dias.get(dia))}
              agora={agora}
              aoAlternarEntrega={aoAlternarEntrega}
              aoAbrir={(registro, elemento) => abrir(dia, registro, elemento)}
              arrastando={arrastando}
              alvo={alvo === dia}
              aceitaSoltura={
                arrastando !== null && dataDoRegistro(arrastando) !== dia
              }
              aoComecarArrasto={comecarArrasto}
              aoTerminarArrasto={terminarArrasto}
              aoEntrarArrasto={() => setAlvo(dia)}
              aoSairArrasto={() => setAlvo((atual) => (atual === dia ? null : atual))}
              aoSoltar={() => soltar(dia)}
            />
          ))}
        </div>
      </div>
      {aberto !== null && registroAberto !== undefined && (
        <PopoverRegistro
          key={`${aberto.dia}-${aberto.tipo}-${aberto.id}`}
          registro={registroAberto}
          ancora={aberto.ancora}
          agora={agora}
          aoAlternarEntrega={aoAlternarEntrega}
          aoEditar={(registro) => {
            fechar()
            aoEditar(registro)
          }}
          aoExcluir={(registro) => {
            fechar()
            aoExcluir(registro)
          }}
          aoFechar={fechar}
        />
      )}
    </>
  )
}
