import { useState } from 'react'
import { CircleAlert } from 'lucide-react'
import type { HabitoNaHome } from '../../api/tipos'
import { useHoje } from '../../lib/useHoje'
import SheetNovoRegistro from '../agenda/SheetNovoRegistro'
import type { AbaNovoRegistro } from '../agenda/SheetNovoRegistro'
import SheetGerenciarHabitos from '../habitos/SheetGerenciarHabitos'
import SheetHabito from '../habitos/SheetHabito'
import AcoesHabitos from './AcoesHabitos'
import CabecalhoHoje from './CabecalhoHoje'
import EstadoVazioHabitos from './EstadoVazioHabitos'
import ListaHabitosDoDia from './ListaHabitosDoDia'
import { mensagemErroMarcacao } from './regras'
import SecaoUpcoming from './SecaoUpcoming'
import { useInicio } from './useInicio'
import { useMarcarHabito } from './useMarcarHabito'
import styles from './PaginaInicio.module.css'

// Qual sheet de hábitos está aberto, se algum.
type SheetAberto = 'novo' | 'gerenciar' | null

export default function PaginaInicio() {
  const hoje = useHoje()
  const inicio = useInicio(hoje)
  const [aviso, setAviso] = useState<string | null>(null)
  const [sheet, setSheet] = useState<SheetAberto>(null)
  // Nome que o formulário de novo hábito já traz (os exemplos da Home vazia).
  const [nomeInicial, setNomeInicial] = useState('')
  // Aba do sheet de novo registro; nulo com o sheet fechado.
  const [registro, setRegistro] = useState<AbaNovoRegistro | null>(null)
  const marcar = useMarcarHabito((erro, marcacao) =>
    setAviso(mensagemErroMarcacao(erro, marcacao.nome)),
  )

  // O alvo é o contrário do que o cartão mostra agora, já com a marcação
  // otimista aplicada. O aviso anterior some no próximo clique.
  function alternar(habito: HabitoNaHome) {
    setAviso(null)
    marcar.mutate({
      id: habito.id,
      nome: habito.nome,
      feito: !habito.feito,
      data: hoje,
    })
  }

  // Sempre passa o nome (vazio por padrão): um exemplo escolhido antes não
  // pode voltar preenchido numa próxima abertura.
  function abrirNovoHabito(nome = '') {
    setNomeInicial(nome)
    setSheet('novo')
  }

  // Sem hábitos o convite do estado vazio já tem o botão de criar.
  const semHabitos = inicio.isSuccess && inicio.data.habitos.length === 0

  return (
    <>
      <div className={styles.pagina}>
        {/* Coluna Today. */}
        <section className={styles.hoje}>
          <CabecalhoHoje data={hoje} />

          {/* O resto da coluna é a segunda linha do grid: o cabeçalho de Today e o
              título de Upcoming dividem a primeira, e os primeiros cartões das duas
              colunas começam juntos. */}
          <div className={styles.corpoHoje}>
            {aviso && (
              <div className={styles.aviso} role="alert">
                <CircleAlert size={16} strokeWidth={1.5} />
                <span>{aviso}</span>
              </div>
            )}

            {inicio.isPending ? (
              <p className={styles.estado}>Carregando…</p>
            ) : inicio.isError ? (
              <div className={styles.estado}>
                <p>Não foi possível carregar seus hábitos.</p>
                <button
                  className={styles.botao}
                  type="button"
                  onClick={() => void inicio.refetch()}
                >
                  Tentar de novo
                </button>
              </div>
            ) : semHabitos ? (
              <EstadoVazioHabitos
                aoNovo={() => abrirNovoHabito()}
                aoEscolherExemplo={abrirNovoHabito}
              />
            ) : (
              <ListaHabitosDoDia
                habitos={inicio.data.habitos}
                aoAlternar={alternar}
              />
            )}

            {!semHabitos && (
              <AcoesHabitos
                aoNovo={() => abrirNovoHabito()}
                aoEditar={() => setSheet('gerenciar')}
              />
            )}
          </div>
        </section>

        {/* Coluna Upcoming: abaixo do Today no mobile e no tablet. */}
        {inicio.data && (
          <SecaoUpcoming
            upcoming={inicio.data.upcoming}
            hoje={hoje}
            aoNovaEntrega={() => setRegistro('ENTREGA')}
            aoNovoEvento={() => setRegistro('EVENTO')}
          />
        )}
      </div>

      <SheetHabito
        aberto={sheet === 'novo'}
        aoFechar={() => setSheet(null)}
        nomeInicial={nomeInicial}
      />
      <SheetGerenciarHabitos
        aberto={sheet === 'gerenciar'}
        aoFechar={() => setSheet(null)}
      />
      <SheetNovoRegistro
        aberto={registro !== null}
        aoFechar={() => setRegistro(null)}
        abaInicial={registro ?? undefined}
      />
    </>
  )
}
