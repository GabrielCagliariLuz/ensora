import { useState } from 'react'
import BlocoAviso from '../../components/ui/BlocoAviso'
import CartaoAjuste from './CartaoAjuste'
import { mensagemErroFuso } from './regras'
import SeletorFuso from './SeletorFuso'
import { useAlterarFuso } from './useAlterarFuso'
import { useConta } from './useConta'
import styles from './CartaoAjuste.module.css'

// E-mail (só leitura) e fuso horário da conta.
export default function SecaoConta() {
  const conta = useConta()
  const alterarFuso = useAlterarFuso()
  // O que a pessoa escolheu e ainda não salvou; null enquanto não mexeu.
  const [escolhido, setEscolhido] = useState<string | null>(null)

  if (conta.isPending) {
    return (
      <CartaoAjuste titulo="Conta">
        <p className={styles.texto}>Carregando…</p>
      </CartaoAjuste>
    )
  }
  if (conta.isError) {
    return (
      <CartaoAjuste titulo="Conta">
        <p className={styles.texto}>Não foi possível carregar a conta.</p>
        <div className={styles.acoes}>
          <button
            className={styles.botao}
            type="button"
            onClick={() => void conta.refetch()}
          >
            Tentar de novo
          </button>
        </div>
      </CartaoAjuste>
    )
  }

  const salvo = conta.data.fuso
  const fuso = escolhido ?? salvo
  const mudou = fuso !== salvo

  function salvar() {
    alterarFuso.mutate(fuso, { onSuccess: () => setEscolhido(null) })
  }

  return (
    <CartaoAjuste titulo="Conta">
      <div className={styles.linha}>
        <span className={styles.rotuloLinha}>E-mail</span>
        <span className={styles.valorLinha}>{conta.data.email}</span>
      </div>

      <SeletorFuso
        id="ajustes-fuso"
        atual={salvo}
        valor={fuso}
        aoMudar={(novo) => {
          alterarFuso.reset()
          setEscolhido(novo)
        }}
      />

      {alterarFuso.isError && (
        <BlocoAviso tom="erro">{mensagemErroFuso(alterarFuso.error)}</BlocoAviso>
      )}

      <div className={styles.acoes}>
        <button
          className={styles.botaoPrimario}
          type="button"
          onClick={salvar}
          disabled={!mudou || alterarFuso.isPending}
        >
          {alterarFuso.isPending ? 'Salvando…' : 'Salvar fuso'}
        </button>
        {alterarFuso.isSuccess && !mudou && (
          <p className={styles.status} role="status">
            Fuso atualizado.
          </p>
        )}
      </div>
    </CartaoAjuste>
  )
}
