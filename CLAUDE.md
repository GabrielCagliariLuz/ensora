# Ensora

PWA pessoal de hábitos, entregas e eventos, com visual analógico
"ink-on-paper". Monorepo:

- `frontend/`: React + Vite + TypeScript (ver `frontend/CLAUDE.md`).
- `backend/`: Spring Boot + PostgreSQL (ver `backend/CLAUDE.md`).

## Fonte de verdade

@ARCHITECTURE.md

- O `ARCHITECTURE.md` manda. Se um pedido ou o código contradiz o
  documento, aponte o conflito antes de seguir.
- Decisão nova ou alterada vai para o `ARCHITECTURE.md` (seção de
  decisões) no mesmo PR da mudança.

## Colaboração

- Só edite código ou configuração quando o Gabriel pedir. Antes de
  editar, diga o que vai mudar e espere a confirmação.
- Ao propor arquitetura, explique o raciocínio e as alternativas.
- Nenhuma decisão de arquitetura silenciosa.
- Não adicione biblioteca (npm ou Maven) sem perguntar.
- Nunca edite uma migração Flyway já aplicada; crie uma nova.
- Tudo em português: respostas, nomes de domínio no código, commits e
  documentação. Termos técnicos (Controller, Service, hook) ficam como
  são.

## Git

- Commits no padrão Conventional Commits, com descrição em português:
  `feat: cria cadastro de hábito`, `fix:`, `docs:`, `refactor:`,
  `test:`, `chore:`.
- Branches a partir da `main`: `feat/...`, `fix/...`, `docs/...`,
  `chore/...`.
- Um assunto por commit. Integração na `main` por pull request.
