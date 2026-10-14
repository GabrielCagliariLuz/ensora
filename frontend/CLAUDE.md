# Frontend do Ensora

Editado no VS Code. Regras gerais e arquitetura: `../CLAUDE.md` e
`../ARCHITECTURE.md` (seção Frontend).

## Stack

- React 19.2, TypeScript ~6.0, Vite 8.3, ESLint 10.
- CSS Modules, sem Tailwind e sem biblioteca de componentes pronta.
- TanStack Query: decidido, ainda não instalado.
- Vitest: a configurar.
- PWA (service worker, cache e fila offline): a configurar.

## Comandos

```bash
npm install       # instala dependências
npm run dev       # servidor de desenvolvimento
npm run build     # checa tipos (tsc -b) e gera o build
npm run lint      # ESLint
npm run preview   # serve o build localmente
npm test          # a configurar (Vitest)
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
```

## Regras

- Componentes não chamam a API. Eles usam os hooks de dados da própria
  feature, e os hooks usam TanStack Query sobre o cliente de `api/`.
- Marcar hábito usa atualização otimista.
- Cada componente tem seu `Componente.module.css` ao lado.
- Cores, fontes e espaçamentos vêm de variáveis CSS globais (`:root`).
  Nada de valor solto de cor ou fonte nos módulos.
- Paleta: paper, canvas, ink, rule, ash, ember, moss, indigo. Fontes:
  IBM Plex Mono e IBM Plex Sans.
- Nomes de domínio em português (`ListaHabitos`, `useHabitos`); termos
  técnicos de layout seguem o documento (`AppShell`, `BottomNav`).
- Token de acesso só em memória. Nunca em localStorage ou
  sessionStorage.
