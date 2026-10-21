import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { WifiOff } from 'lucide-react'
import BlocoAviso from '../../components/ui/BlocoAviso'
import CampoHorario from '../../components/ui/CampoHorario'
import CampoTexto from '../../components/ui/CampoTexto'
import { hojeISO } from '../../lib/datas'
import { useOnline } from '../../lib/useOnline'
import {
  CAMPOS_ENTREGA,
  errosDeCamposEntrega,
  mensagemErroRegistro,
  montarEntrega,
  validarEntrega,
} from './regras'
import type { CampoEntrega, ErrosEntrega } from './regras'
import { useCriarEntrega } from './useCriarEntrega'
import styles from './FormularioRegistro.module.css'

interface FormularioEntregaProps {
  aoConcluir: () => void
  aoCancelar: () => void
}

export default function FormularioEntrega({
  aoConcluir,
  aoCancelar,
}: FormularioEntregaProps) {
  const id = useId()
  const idCampo = (campo: CampoEntrega) => `${id}-${campo}`

  const [titulo, setTitulo] = useState('')
  const [prazoData, setPrazoData] = useState(hojeISO)
  const [prazoHorario, setPrazoHorario] = useState('')
  const [nota, setNota] = useState('')
  const [errosLocais, setErrosLocais] = useState<ErrosEntrega>({})

  const online = useOnline()
  const criar = useCriarEntrega()

  const errosServidor = errosDeCamposEntrega(criar.error)
  const erroDoCampo = (campo: CampoEntrega) =>
    errosLocais[campo] ?? errosServidor[campo]
  const erroGeral =
    criar.error !== null && Object.keys(errosServidor).length === 0
      ? mensagemErroRegistro(criar.error)
      : null

  // Corrigir um campo apaga só o erro dele.
  function limpar(campo: CampoEntrega) {
    setErrosLocais((atuais) => ({ ...atuais, [campo]: undefined }))
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const valores = { titulo, prazoData, prazoHorario, nota }
    const erros = validarEntrega(valores)
    setErrosLocais(erros)

    const primeiro = CAMPOS_ENTREGA.find((campo) => erros[campo])
    if (primeiro) return document.getElementById(idCampo(primeiro))?.focus()

    criar.mutate(montarEntrega(valores), { onSuccess: aoConcluir })
  }

  return (
    <form className={styles.formulario} onSubmit={enviar} noValidate>
      {!online && (
        <BlocoAviso tom="aviso" icone={WifiOff}>
          Sem conexão. Conecte-se para salvar o registro.
        </BlocoAviso>
      )}
      {erroGeral && <BlocoAviso tom="erro">{erroGeral}</BlocoAviso>}

      <CampoTexto
        id={idCampo('titulo')}
        rotulo="Nome da entrega"
        placeholder="Ex: Trabalho da faculdade"
        valor={titulo}
        aoMudar={(valor) => {
          setTitulo(valor)
          limpar('titulo')
        }}
        erro={erroDoCampo('titulo')}
        focoInicial
      />

      <div className={styles.linha}>
        <CampoTexto
          id={idCampo('prazoData')}
          rotulo="Data limite"
          tipo="date"
          valor={prazoData}
          aoMudar={(valor) => {
            setPrazoData(valor)
            limpar('prazoData')
          }}
          erro={erroDoCampo('prazoData')}
        />
        <CampoHorario
          id={idCampo('prazoHorario')}
          rotulo="Horário limite"
          opcional
          placeholder="23:59"
          valor={prazoHorario}
          aoMudar={(valor) => {
            setPrazoHorario(valor)
            limpar('prazoHorario')
          }}
          erro={erroDoCampo('prazoHorario')}
          apoio="Sem horário, vale até 23:59."
        />
      </div>

      <CampoTexto
        id={idCampo('nota')}
        rotulo="Nota"
        opcional
        multilinha
        placeholder="Ex: Capítulos 1 a 4"
        valor={nota}
        aoMudar={(valor) => {
          setNota(valor)
          limpar('nota')
        }}
        erro={erroDoCampo('nota')}
      />

      <div className={styles.acoes}>
        <button className={styles.cancelar} type="button" onClick={aoCancelar}>
          Cancelar
        </button>
        <button
          className={styles.salvar}
          type="submit"
          disabled={!online || criar.isPending}
        >
          {criar.isPending ? 'Salvando…' : 'Inserir Entrega'}
        </button>
      </div>
    </form>
  )
}
