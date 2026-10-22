import { useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { CircleAlert, WifiOff } from 'lucide-react'
import ControleSegmentado from '../../components/ui/ControleSegmentado'
import { hojeISO } from '../../lib/datas'
import { focoAutomatico } from '../../lib/foco'
import { useOnline } from '../../lib/useOnline'
import type { Habito, TipoHabito } from '../../api/tipos'
import StepperMeta from './StepperMeta'
import { useCriarHabito } from './useCriarHabito'
import { useEditarHabito } from './useEditarHabito'
import {
  errosDeCampos,
  META_PADRAO,
  mensagemErroHabito,
  montarRequisicao,
  validarHabito,
} from './regras'
import type { ErrosHabito } from './regras'
import styles from './FormularioHabito.module.css'

const OPCOES_FREQUENCIA: { valor: TipoHabito; rotulo: string }[] = [
  { valor: 'DIARIO', rotulo: 'Diário' },
  { valor: 'SEMANAL', rotulo: 'Semanal' },
]

interface FormularioHabitoProps {
  // Sem hábito é criação; com hábito é edição (o tipo não muda).
  habito?: Habito
  aoConcluir: () => void
  aoCancelar: () => void
}

export default function FormularioHabito({
  habito,
  aoConcluir,
  aoCancelar,
}: FormularioHabitoProps) {
  const editando = habito !== undefined
  const id = useId()
  const idNome = `${id}-nome`
  const idFrequencia = `${id}-frequencia`
  const idMeta = `${id}-meta`

  const [nome, setNome] = useState(habito?.nome ?? '')
  const [tipo, setTipo] = useState<TipoHabito>(habito?.tipo ?? 'DIARIO')
  const [meta, setMeta] = useState(habito?.vezesPorSemana ?? META_PADRAO)
  const [errosLocais, setErrosLocais] = useState<ErrosHabito>({})
  const campoNome = useRef<HTMLInputElement | null>(null)

  const online = useOnline()
  const criar = useCriarHabito()
  const editar = useEditarHabito()
  const pendente = criar.isPending || editar.isPending
  const erro = editando ? editar.error : criar.error

  const errosServidor = errosDeCampos(erro)
  const erroNome = errosLocais.nome ?? errosServidor.nome
  const erroMeta = errosServidor.vezesPorSemana
  const erroGeral =
    erro !== null && Object.keys(errosServidor).length === 0
      ? mensagemErroHabito(erro)
      : null

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const valores = { nome, tipo, vezesPorSemana: meta }
    const erros = validarHabito(valores)
    setErrosLocais(erros)
    if (erros.nome) return campoNome.current?.focus()

    const corpo = montarRequisicao(valores, hojeISO())
    if (habito) {
      editar.mutate({ id: habito.id, corpo }, { onSuccess: aoConcluir })
    } else {
      criar.mutate(corpo, { onSuccess: aoConcluir })
    }
  }

  return (
    <form className={styles.formulario} onSubmit={enviar} noValidate>
      {!online && (
        <div className={styles.aviso} role="status">
          <WifiOff size={16} strokeWidth={1.5} />
          <span>Sem conexão. Conecte-se para salvar o hábito.</span>
        </div>
      )}

      {erroGeral && (
        <div className={styles.alerta} role="alert">
          <CircleAlert size={16} strokeWidth={1.5} />
          <span>{erroGeral}</span>
        </div>
      )}

      <div className={styles.campo}>
        <label className={styles.rotulo} htmlFor={idNome}>
          Nome do hábito
        </label>
        <input
          // O <dialog> só aceita foco depois de aberto, então o foco inicial
          // vem do atributo autofocus, que o showModal() respeita. O autoFocus
          // do React chamaria focus() com o diálogo ainda fechado.
          ref={(elemento) => {
            campoNome.current = elemento
            if (focoAutomatico()) elemento?.setAttribute('autofocus', '')
          }}
          id={idNome}
          className={styles.entrada}
          type="text"
          placeholder="Ex: Meditar 10 minutos"
          autoComplete="off"
          value={nome}
          onChange={(e) => {
            setNome(e.target.value)
            setErrosLocais({})
          }}
          aria-invalid={erroNome ? true : undefined}
          aria-describedby={erroNome ? `${idNome}-erro` : undefined}
        />
        {erroNome && (
          <span id={`${idNome}-erro`} className={styles.dica}>
            {erroNome}
          </span>
        )}
      </div>

      <div className={styles.campo}>
        <span id={idFrequencia} className={styles.rotulo}>
          Frequência
        </span>
        <ControleSegmentado
          idRotulo={idFrequencia}
          opcoes={OPCOES_FREQUENCIA}
          valor={tipo}
          aoMudar={setTipo}
          desabilitado={editando}
        />
        {tipo === 'DIARIO' && (
          <p className={styles.apoio}>Repete todos os dias automaticamente</p>
        )}
      </div>

      {tipo === 'SEMANAL' && (
        <div className={styles.campo}>
          <span id={idMeta} className={styles.rotulo}>
            Meta semanal
          </span>
          <StepperMeta idRotulo={idMeta} valor={meta} aoMudar={setMeta} />
          {erroMeta ? (
            <span className={styles.dica}>{erroMeta}</span>
          ) : (
            <p className={styles.apoio}>
              {editando
                ? 'A nova meta vale a partir da próxima semana.'
                : 'Distribua os dias livremente ao longo da semana'}
            </p>
          )}
        </div>
      )}

      <div className={styles.acoes}>
        <button className={styles.cancelar} type="button" onClick={aoCancelar}>
          Cancelar
        </button>
        <button
          className={styles.salvar}
          type="submit"
          disabled={!online || pendente}
        >
          {pendente
            ? 'Salvando…'
            : editando
              ? 'Salvar'
              : 'Adicionar hábito'}
        </button>
      </div>
    </form>
  )
}
