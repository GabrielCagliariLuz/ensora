import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { entrar } from '../../api/auth'
import { CAMINHOS } from '../../lib/caminhos'

export function useEntrar() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: entrar,
    onSuccess: () => {
      void navigate(CAMINHOS.inicio, { replace: true })
    },
  })
}
