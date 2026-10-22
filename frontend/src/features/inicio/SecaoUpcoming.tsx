import { useState } from 'react'
import type { Entrega, Upcoming } from '../../api/tipos'
import BlocoAviso from '../../components/ui/BlocoAviso'
import { useAgora } from '../../lib/useAgora'
import { useAnimarOrdem } from '../../lib/useAnimarOrdem'
import ItemEntrega from './ItemEntrega'
import ItemEvento from './ItemEvento'
import { mensagemErroConclusao } from './regras'
import { ordenarEntregas } from './upcoming'
import { useConcluirEntrega } from './useConcluirEntrega'
import styles from './SecaoUpcoming.module.css'

interface SecaoUpcomingProps {
  upcoming: Upcoming
  // Dia de hoje (AAAA-MM-DD) da Home: chave do cache e data da conclusão.
  hoje: string
}

export default function SecaoUpcoming({ upcoming, hoje }: SecaoUpcomingProps) {
  const agora = useAgora()
  const [aviso, setAviso] = useState<string | null>(null)
  const concluir = useConcluirEntrega((erro, conclusao) =>
    setAviso(mensagemErroConclusao(erro, conclusao.titulo)),
  )

  const entregas = ordenarEntregas(upcoming.entregas)
  const eventos = upcoming.eventos
  // Concluir faz a entrega descer: a lista desliza em vez de pular.
  const registrarEntrega = useAnimarOrdem(entregas.map((e) => e.id).join(','))
  const vazio = entregas.length === 0 && eventos.length === 0

  // O alvo é o contrário do que o cartão mostra agora, já com a conclusão
  // otimista aplicada. O aviso anterior some no próximo clique.
  function alternar(entrega: Entrega) {
    setAviso(null)
    concluir.mutate({
      id: entrega.id,
      titulo: entrega.titulo,
      concluida: !entrega.concluida,
      data: hoje,
    })
  }

  return (
    <section className={styles.secao}>
      <h2 className={styles.titulo}>Upcoming</h2>

      {/* Entregas e eventos são blocos separados, cada um no seu cartão. */}
      <div className={styles.blocos}>
        {aviso && <BlocoAviso tom="erro">{aviso}</BlocoAviso>}

        {vazio && (
          <div className={styles.cartao}>
            <p className={styles.vazio}>Nada nos próximos 4 dias.</p>
          </div>
        )}

        {entregas.length > 0 && (
          <div className={styles.cartao}>
            <h3 className={styles.rotulo}>Entregas &amp; Prazos</h3>
            <ul className={styles.lista}>
              {entregas.map((entrega) => (
                <li key={entrega.id} ref={registrarEntrega(entrega.id)}>
                  <ItemEntrega
                    entrega={entrega}
                    agora={agora}
                    aoAlternar={() => alternar(entrega)}
                  />
                </li>
              ))}
            </ul>
          </div>
        )}

        {eventos.length > 0 && (
          <div className={styles.cartao}>
            <h3 className={styles.rotulo}>Eventos &amp; Compromissos</h3>
            <ul className={styles.lista}>
              {eventos.map((evento) => (
                <li key={evento.id}>
                  <ItemEvento evento={evento} hoje={hoje} agora={agora} />
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </section>
  )
}
