interface AnelEnsoraProps {
  className?: string
}

export default function AnelEnsora({ className }: AnelEnsoraProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="3.5"
      strokeLinecap="round"
      aria-hidden="true"
    >
      <circle
        cx="12"
        cy="12"
        r="9"
        strokeDasharray="46 10.55"
        transform="rotate(123 12 12)"
      />
    </svg>
  )
}
