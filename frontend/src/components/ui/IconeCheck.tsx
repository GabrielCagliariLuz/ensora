interface IconeCheckProps {
  size?: number
}

// Check do Figma (ic_check). Ícone próprio, e não do lucide: o glifo do
// protótipo tem a haste longa e as pontas arredondadas, e o Check do lucide
// é outro desenho. A cor vem do texto do pai (currentColor).
export default function IconeCheck({ size = 12 }: IconeCheckProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 12 12"
      fill="none"
      aria-hidden="true"
    >
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        fill="currentColor"
        d="M11.5384 0.206298C11.0394 -0.144284 10.3414 -0.0370163 9.97798 0.44438L4.78894 9.28207L1.93058 6.02902C1.50946 5.59472 0.802782 5.57379 0.352815 5.98193C-0.0971515 6.38745 -0.120227 7.0703 0.30378 7.50199C0.30378 7.50199 3.77652 11.4431 4.27552 11.7937C4.77452 12.1443 5.47255 12.037 5.83598 11.5556L11.7865 1.71066C12.1499 1.22665 12.0374 0.554264 11.5384 0.206298Z"
      />
    </svg>
  )
}
