# Frontend do Ensora

Editado no VS Code. Regras gerais e arquitetura: `../CLAUDE.md` e
`../ARCHITECTURE.md` (seção Frontend).

## Stack

- React 19.2, TypeScript ~6.0, Vite 8.3, ESLint 10.
- CSS Modules, sem Tailwind e sem biblioteca de componentes pronta.
- Ícones: lucide-react.
- TanStack Query (`QueryClient` em `app/queryClient.ts`) e React Router
  (`react-router`, `createBrowserRouter`).
- Vitest (ambiente `node`; testes `*.test.ts` ao lado do código, importando
  `describe/it/expect/vi` de `vitest`).
- PWA: `vite-plugin-pwa` (manifest, ícones e service worker com o shell em
  cache, `autoUpdate`). Ícones gerados por `npm run icones` a partir de
  `public/anel.svg`; os PNGs são commitados. Cache de dados e fila offline
  ainda não existem (#56 e #57).

## Comandos

```bash
npm install       # instala dependências
npm run dev       # servidor de desenvolvimento
npm run build     # checa tipos (tsc -b) e gera o build
npm run lint      # ESLint
npm run preview   # serve o build localmente
npm test          # roda os testes uma vez (Vitest)
npm run test:watch  # Vitest em modo observação
```

## Estrutura de `src/`

```text
app/          rotas, providers, QueryClient
components/
  ui/         componentes reutilizáveis (bottom sheets, modal)
  layout/     AppShell, BottomNav, Sidebar
features/     uma pasta por funcionalidade (auth, inicio, habitos, calendario, insights)
api/          cliente HTTP e tipos das respostas
lib/          datas e utilitários
styles/       tokens globais (tokens.css, importado por index.css)
```

## Regras

- Componentes não chamam a API. Eles usam os hooks de dados da própria
  feature, e os hooks usam TanStack Query sobre o cliente de `api/`.
- Marcar hábito usa atualização otimista.
- Cada componente tem seu `Componente.module.css` ao lado.
- Cores, fontes e espaçamentos vêm de variáveis CSS globais (`:root`).
  Nada de valor solto de cor ou fonte nos módulos.
- Tokens em `src/styles/tokens.css`. Cores: canvas, card, hover-surface,
  hairline, rule, ink, ink-hover, graphite, ash, paper, moss (+ surface,
  border), ember (+ surface, border) e amber (+ surface, border). Sem tema escuro. Fontes: IBM Plex
  Sans (interface) e IBM Plex Mono (números e rótulos), via Google Fonts.
- Ícones: importar sempre por ícone (`import { Plus } from 'lucide-react'`),
  nunca o pacote inteiro. Usar `strokeWidth={1.5}` e o tamanho pela prop
  `size`. A cor vem do texto do pai (`currentColor`), sem cor solta.
- Nomes de domínio em português (`ListaHabitos`, `useHabitos`); termos
  técnicos de layout seguem o documento (`AppShell`, `BottomNav`).
- Rótulos de navegação e títulos de seção seguem o Figma, em inglês
  (Today, Insights, Calendar, Settings; títulos em mono maiúsculo). As
  URLs ficam em português (`/calendario`, `/ajustes`). Telas de
  referência em `../docs/telas/`.
- Token de acesso só em memória. Nunca em localStorage ou
  sessionStorage.
- Toda chamada à API passa por `requisitar` (`api/cliente.ts`). Em 401 ele
  renova a sessão (uma renovação por vez, pois o refresh token é rotativo)
  e repete a requisição uma vez. `/auth/*` não recebe `Authorization` nem
  dispara renovação. A base vem de `VITE_API_URL` (ver `.env.example`).
- Sessão perdida (refresh recusado): o cliente limpa o token e chama o
  callback de `definirAoSessaoExpirar`, registrado em `app/`. `api/` não
  conhece o router. Falha de rede vira `ErroApi` com `status: 0`.
