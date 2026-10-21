import IconeCheck from './IconeCheck'
import styles from './CaixaMarcacao.module.css'

interface CaixaMarcacaoProps {
  marcada: boolean
  // 'pequeno' é a das etiquetas do Calendar; 'normal', a dos cartões.
  tamanho?: 'normal' | 'pequeno'
}

// Só o desenho da caixa de seleção. Quem usa põe o <input type="checkbox"> real
// (invisível, por cima) para ter clique, foco e leitor de tela.
export default function CaixaMarcacao({
  marcada,
  tamanho = 'normal',
}: CaixaMarcacaoProps) {
  const classes = [
    styles.caixa,
    tamanho === 'pequeno' && styles.pequena,
    marcada && styles.marcada,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <span className={classes} aria-hidden="true">
      <IconeCheck size={tamanho === 'pequeno' ? 8 : 12} />
    </span>
  )
}
