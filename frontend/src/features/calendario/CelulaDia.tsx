import type { Entrega } from '../../api/tipos'
import { numeroDoDia } from '../../lib/datas'
import { mesDoDia } from './grade'
import { idDoRegistro } from './mover'
import type { Registro } from './agenda'
import EtiquetaEntrega from './EtiquetaEntrega'
import EtiquetaEvento from './EtiquetaEvento'
import styles from './CelulaDia.module.css'

interface CelulaDiaProps {
  dia: string
  // Mês que a grade mostra: os dias dos meses vizinhos ficam esmaecidos.
  mesVisivel: string
  hoje: string
  registros: Registro[]
  agora: Date
  aoAlternarEntrega: (entrega: Entrega) => void
  aoAbrir: (registro: Registro, elemento: HTMLElement) => void
  // Registro em arraste (de qualquer dia): fica esmaecido onde está.
  arrastando: Registro | null
  // O dia está sob o cursor e aceita a soltura.
  alvo: boolean
  aoComecarArrasto: (registro: Registro) => void
  aoTerminarArrasto: () => void
  aoEntrarArrasto: () => void
  aoSairArrasto: () => void
  aoSoltar: () => void
  // Aceita a soltura aqui? (Não aceita no dia em que o registro já está.)
  aceitaSoltura: boolean
}

export default function CelulaDia({
  dia,
  mesVisivel,
  hoje,
  registros,
  agora,
  aoAlternarEntrega,
  aoAbrir,
  arrastando,
  alvo,
  aoComecarArrasto,
  aoTerminarArrasto,
  aoEntrarArrasto,
  aoSairArrasto,
  aoSoltar,
  aceitaSoltura,
}: CelulaDiaProps) {
  const foraDoMes = mesDoDia(dia) !== mesVisivel
  const ehHoje = dia === hoje
  const classes = [
    styles.celula,
    foraDoMes && styles.foraDoMes,
    ehHoje && styles.hoje,
    alvo && styles.alvo,
  ]
    .filter(Boolean)
    .join(' ')

  function ehOArrastado(registro: Registro): boolean {
    return (
      arrastando !== null &&
      arrastando.tipo === registro.tipo &&
      idDoRegistro(arrastando) === idDoRegistro(registro)
    )
  }

  return (
    <div
      className={classes}
      onDragEnter={(evento) => {
        if (!aceitaSoltura) return
        evento.preventDefault()
        aoEntrarArrasto()
      }}
      onDragOver={(evento) => {
        // Cancelar o padrão é o que faz a célula aceitar a soltura.
        if (aceitaSoltura) evento.preventDefault()
      }}
      onDragLeave={(evento) => {
        // Passar para um filho da célula também dispara o leave: ignora.
        if (!evento.currentTarget.contains(evento.relatedTarget as Node | null)) {
          aoSairArrasto()
        }
      }}
      onDrop={(evento) => {
        if (!aceitaSoltura) return
        evento.preventDefault()
        aoSoltar()
      }}
    >
      <span className={styles.numero}>{numeroDoDia(dia)}</span>
      <div className={styles.registros}>
        {registros.map((registro) =>
          registro.tipo === 'EVENTO' ? (
            <EtiquetaEvento
              key={`evento-${registro.evento.id}`}
              evento={registro.evento}
              aoAbrir={(elemento) => aoAbrir(registro, elemento)}
              arrastando={ehOArrastado(registro)}
              aoComecarArrasto={() => aoComecarArrasto(registro)}
              aoTerminarArrasto={aoTerminarArrasto}
            />
          ) : (
            <EtiquetaEntrega
              key={`entrega-${registro.entrega.id}`}
              entrega={registro.entrega}
              agora={agora}
              aoAlternar={() => aoAlternarEntrega(registro.entrega)}
              aoAbrir={(elemento) => aoAbrir(registro, elemento)}
              arrastando={ehOArrastado(registro)}
              aoComecarArrasto={() => aoComecarArrasto(registro)}
              aoTerminarArrasto={aoTerminarArrasto}
            />
          ),
        )}
      </div>
    </div>
  )
}
