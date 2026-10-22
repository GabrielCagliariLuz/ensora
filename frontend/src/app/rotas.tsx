import { createBrowserRouter, Navigate } from 'react-router'
import { definirAoSessaoExpirar } from '../api/cliente'
import AppShell from '../components/layout/AppShell'
import TelaCarregando from '../components/ui/TelaCarregando'
import TelaErroConexao from '../components/ui/TelaErroConexao'
import PaginaAjustes from '../features/ajustes/PaginaAjustes'
import PaginaCadastro from '../features/auth/PaginaCadastro'
import PaginaLogin from '../features/auth/PaginaLogin'
import PaginaCalendario from '../features/calendario/PaginaCalendario'
import PaginaInicio from '../features/inicio/PaginaInicio'
import PaginaInsights from '../features/insights/PaginaInsights'
import { CAMINHOS } from '../lib/caminhos'
import { queryClient } from './queryClient'
import { carregarLogin, carregarSessao } from './sessao'

// HydrateFallback aparece enquanto o loader da primeira carga roda, ou seja,
// durante o /auth/refresh (e o cold start do backend).
export const router = createBrowserRouter([
  {
    element: <AppShell />,
    loader: carregarSessao,
    HydrateFallback: TelaCarregando,
    errorElement: <TelaErroConexao />,
    children: [
      { path: CAMINHOS.inicio, element: <PaginaInicio /> },
      { path: CAMINHOS.insights, element: <PaginaInsights /> },
      { path: CAMINHOS.calendario, element: <PaginaCalendario /> },
      { path: CAMINHOS.ajustes, element: <PaginaAjustes /> },
    ],
  },
  {
    path: CAMINHOS.login,
    element: <PaginaLogin />,
    loader: carregarLogin,
    HydrateFallback: TelaCarregando,
    errorElement: <TelaErroConexao />,
  },
  {
    path: CAMINHOS.cadastro,
    element: <PaginaCadastro />,
    loader: carregarLogin,
    HydrateFallback: TelaCarregando,
    errorElement: <TelaErroConexao />,
  },
  { path: '*', element: <Navigate to={CAMINHOS.inicio} replace /> },
])

definirAoSessaoExpirar(() => {
  queryClient.clear()
  void router.navigate(CAMINHOS.login, { replace: true })
})
