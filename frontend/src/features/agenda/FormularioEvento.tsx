import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { WifiOff } from 'lucide-react'
import type { CategoriaEvento } from '../../api/tipos'
import BlocoAviso from '../../components/ui/BlocoAviso'
import CampoHorario from '../../components/ui/CampoHorario'
import CampoTexto from '../../components/ui/CampoTexto'
import { hojeISO } from '../../lib/datas'
import { useOnline } from '../../lib/useOnline'
import {
  CAMPOS_EVENTO,
  errosDeCamposEvento,
  mensagemErroRegistro,
  montarEvento,
  validarEvento,
} from './regras'
import type { CampoEvento, ErrosEvento } from './regras'
import SeletorCategoria from './SeletorCategoria'
import { useCriarEvento } from './useCriarEvento'
import styles from './FormularioRegistro.module.css'

interface FormularioEventoProps {
  aoConcluir: () => void
  aoCancelar: () => void
}

export default function FormularioEvento({
  aoConcluir,
  aoCancelar,
}: FormularioEventoProps) {
  const id = useId()
  const idCategoria = `${id}-categoria`
  const idCampo = (campo: CampoEvento) => `${id}-${campo}`

  const [titulo, setTitulo] = useState('')
  const [data, setData] = useState(hojeISO)
  const [local, setLocal] = useState('')
  const [inicio, setInicio] = useState('')
  const [termino, setTermino] = useState('')
  const [categoria, setCategoria] = useState<CategoriaEvento | null>(null)
  const [errosLocais, setErrosLocais] = useState<ErrosEvento>({})

  const online = useOnline()
  const criar = useCriarEvento()

  const errosServidor = errosDeCamposEvento(criar.error)
  const erroDoCampo = (campo: CampoEvento) =>
    errosLocais[campo] ?? errosServidor[campo]
  const erroGeral =
    criar.error !== null && Object.keys(errosServidor).length === 0
      ? mensagemErroRegistro(criar.error)
      : null

  // Corrigir um campo apaga só o erro dele.
  function limpar(campo: CampoEvento) {
    setErrosLocais((atuais) => ({ ...atuais, [campo]: undefined }))
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()

    const valores = { titulo, data, inicio, termino, local, categoria }
    const erros = validarEvento(valores)
    setErrosLocais(erros)

    const primeiro = CAMPOS_EVENTO.find((campo) => erros[campo])
    if (primeiro) return document.getElementById(idCampo(primeiro))?.focus()

    criar.mutate(montarEvento(valores), { onSuccess: aoConcluir })
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
        rotulo="Nome do evento"
        placeholder="Ex: Aniversário do Matheus"
        valor={titulo}
        aoMudar={(valor) => {
          setTitulo(valor)
          limpar('titulo')
        }}
        erro={erroDoCampo('titulo')}
        focoInicial
      />

      <div className={styles.campo}>
        <span id={idCategoria} className={styles.rotulo}>
          Categoria <span className={styles.opcional}>(opcional)</span>
        </span>
        <SeletorCategoria
          idRotulo={idCategoria}
          valor={categoria}
          aoMudar={setCategoria}
        />
      </div>

      <div className={styles.linha}>
        <CampoTexto
          id={idCampo('data')}
          rotulo="Data"
          tipo="date"
          valor={data}
          aoMudar={(valor) => {
            setData(valor)
            limpar('data')
          }}
          erro={erroDoCampo('data')}
        />
        <CampoTexto
          id={idCampo('local')}
          rotulo="Local"
          opcional
          placeholder="Ex: Prédio 32, sala 204"
          valor={local}
          aoMudar={(valor) => {
            setLocal(valor)
            limpar('local')
          }}
          erro={erroDoCampo('local')}
        />
      </div>

      <div className={styles.linha}>
        <CampoHorario
          id={idCampo('inicio')}
          rotulo="Início"
          placeholder="09:00"
          valor={inicio}
          aoMudar={(valor) => {
            setInicio(valor)
            limpar('inicio')
            limpar('termino')
          }}
          erro={erroDoCampo('inicio')}
        />
        <CampoHorario
          id={idCampo('termino')}
          rotulo="Término"
          opcional
          placeholder="10:00"
          aPartirDe={inicio}
          valor={termino}
          aoMudar={(valor) => {
            setTermino(valor)
            limpar('termino')
          }}
          erro={erroDoCampo('termino')}
        />
      </div>

      <div className={styles.acoes}>
        <button className={styles.cancelar} type="button" onClick={aoCancelar}>
          Cancelar
        </button>
        <button
          className={styles.salvar}
          type="submit"
          disabled={!online || criar.isPending}
        >
          {criar.isPending ? 'Salvando…' : 'Inserir Evento'}
        </button>
      </div>
    </form>
  )
}
