# Frontend do Ensora

SPA em React 19 e TypeScript, construída com Vite. O README do projeto, com a visão geral, está na [raiz do repositório](../README.md), e as decisões de arquitetura em [`ARCHITECTURE.md`](../ARCHITECTURE.md).

## Comandos

```bash
npm install         # instala as dependências
npm run dev         # servidor de desenvolvimento (http://localhost:5173)
npm test            # testes (Vitest)
npm run lint        # ESLint
npm run build       # checa os tipos e gera o build
npm run icones      # regera os ícones do PWA a partir de public/anel.svg
```

A API vem de `VITE_API_URL` (veja `.env.example`). Em desenvolvimento, o padrão é `http://localhost:8080`; no build de produção, o `Dockerfile` usa `/api`, na mesma origem da SPA.

Regras de estrutura e convenções do front: [`CLAUDE.md`](CLAUDE.md).
