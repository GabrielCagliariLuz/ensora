import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { WifiOff } from 'lucide-react'
import type { Entrega } from '../../api/tipos'
import BlocoAviso from '../../components/ui/BlocoAviso'
import CampoHorario from '../../components/ui/CampoHorario'
import CampoTexto from '../../components/ui/CampoTexto'
import { hojeISO } from '../../lib/datas'
import { useOnline } from '../../lib/useOnline'
import { horaCurta } from '../calendario/agenda'
import {
  CAMPOS_ENTREGA,
  errosDeCamposEntrega,
  mensagemErroRegistro,
  montarEntrega,
  validarEntrega,
} from './regras'
import type { CampoEntrega, ErrosEntrega } from './regras'
import { useSalvarEntrega } from './useEditarRegistro'
import styles from './FormularioRegistro.module.css'

interface FormularioEntregaProps {
  // Com ela o formulário edita essa entrega; sem, cria uma nova.
  existente?: Entrega
  // Data limite preenchida numa entrega nova; sem ela, hoje.
  dataInicial?: string
  aoConcluir: () => void
  aoCancelar: () => void
}

export default function FormularioEntrega({
  existente,
  dataInicial,
  aoConcluir,
  aoCancelar,
}: FormularioEntregaProps) {
  const id = useId()
  const idCampo = (campo: CampoEntrega) => `${id}-${campo}`

  const [titulo, setTitulo] = useState(existente?.titulo ?? '')
  const [prazoData, setPrazoData] = useState(
    existente?.prazoData ?? dataInicial ?? hojeISO(),
  )
  const [prazoHorario, setPrazoHorario] = useState(
    existente?.prazoHorario ? horaCurta(existente.prazoHorario) : '',
  )
  const [nota, setNota] = useState(existente?.nota ?? '')
  const [errosLocais, setErrosLocais] = useState<ErrosEntrega>({})

  const online = useOnline()
  const salvar = useSalvarEntrega(existente)

  const errosServidor = errosDeCamposEntrega(salvar.error)
  const erroDoCampo = (campo: CampoEntrega) =>
    errosLocais[campo] ?? errosServidor[campo]
  const erroGeral =
    salvar.error !== null && Object.keys(errosServidor).length === 0
      ? mensagemErroRegistro(salvar.error)
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

    salvar.mutate(montarEntrega(valores), { onSuccess: aoConcluir })
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
          disabled={!online || salvar.isPending}
        >
          {salvar.isPending
            ? 'Salvando…'
            : existente
              ? 'Salvar'
              : 'Inserir Entrega'}
        </button>
      </div>
    </form>
  )
}
