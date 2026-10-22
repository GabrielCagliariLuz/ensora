import CabecalhoPagina from '../../components/ui/CabecalhoPagina'
import SecaoConta from './SecaoConta'
import SecaoExcluirConta from './SecaoExcluirConta'
import SecaoSenha from './SecaoSenha'
import SecaoSessao from './SecaoSessao'
import styles from './PaginaAjustes.module.css'

export default function PaginaAjustes() {
  return (
    <>
      <CabecalhoPagina titulo="Settings" legenda="Conta e preferências." />
      <div className={styles.secoes}>
        <SecaoConta />
        <SecaoSenha />
        <SecaoSessao />
        <SecaoExcluirConta />
      </div>
    </>
  )
}
