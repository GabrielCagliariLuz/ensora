import { createBrowserRouter, Navigate } from 'react-router'
import AppShell from '../components/layout/AppShell'
import PaginaAjustes from '../features/ajustes/PaginaAjustes'
import PaginaLogin from '../features/auth/PaginaLogin'
import PaginaCalendario from '../features/calendario/PaginaCalendario'
import PaginaInicio from '../features/inicio/PaginaInicio'
import PaginaInsights from '../features/insights/PaginaInsights'
import { CAMINHOS } from '../lib/caminhos'

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: CAMINHOS.inicio, element: <PaginaInicio /> },
      { path: CAMINHOS.insights, element: <PaginaInsights /> },
      { path: CAMINHOS.calendario, element: <PaginaCalendario /> },
      { path: CAMINHOS.ajustes, element: <PaginaAjustes /> },
    ],
  },
  { path: CAMINHOS.login, element: <PaginaLogin /> },
  { path: '*', element: <Navigate to={CAMINHOS.inicio} replace /> },
])
