import { RouterProvider } from 'react-router'
import Providers from './Providers'
import { router } from './rotas'

export default function App() {
  return (
    <Providers>
      <RouterProvider router={router} />
    </Providers>
  )
}
