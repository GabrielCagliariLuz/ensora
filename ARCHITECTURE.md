# Ensora --- Arquitetura

> App pessoal de organização, foco, rituais e hábitos. Primeiro uso
> pessoal no celular e computador (PWA), com possibilidade de evoluir
> para multiusuário no futuro.

## 1. Princípios

1.  **Frontend** mostra e coleta; backend decide e calcula; banco
    guarda.
2.  **Cada módulo tem uma responsabilidade clara.**
3.  **Dados não são resetados.** O "reset" diário/semanal é apenas uma
    visão dos registros existentes.
4.  **Uso pessoal primeiro.** Decisões técnicas não devem ser
    complicadas apenas para antecipar um produto multiusuário.
5.  **O assistente de código (Claude Code) deve ajudar a pensar,
    revisar e corrigir.** Não deve tomar decisões de arquitetura sem
    apresentar o raciocínio quando houver impacto relevante.

## 2. Stack

---

Camada Tecnologia

---

Frontend React + Vite + TypeScript, PWA, CSS
Modules, TanStack Query

Backend Spring Boot, Spring Security + JWT

Banco PostgreSQL (Neon)

Frontend hosting Vercel

Backend hosting Render

---

## 3. Mapa do sistema

```text
[PWA React - Vercel] -- HTTPS/JSON + JWT --> [API Spring Boot - Render] -- JDBC --> [PostgreSQL - Neon]
```

- **PWA:** telas, interação e cache de dados no cliente.
- **API:** autenticação, regras de negócio e cálculos de Insights.
- **Banco:** persistência dos dados.
- O sistema deve estar preparado para sincronizar os dados entre
  dispositivos quando houver autenticação.

## 4. Conceitos de domínio

---

Conceito Descrição

---

**Usuário** Dono dos dados. Inicialmente haverá
apenas um usuário.

**Hábito** Atividade recorrente diária ou
semanal. Nesta versão é somente
binário: feito/não feito.

**Registro de conclusão** Registro de que um hábito foi feito
em uma determinada data. É a base
da Home e dos Insights.

**Entrega** Item com prazo (data e horário
opcional) e estado feito/não
feito.

**Evento** Compromisso com data e horário,
categoria e local opcional,
apenas informativo, sem estado de
conclusão.

---

Entrega e evento são **entidades separadas**. O Calendar apenas reúne as
duas na leitura.

## 5. Regras de hábitos

### 5.1 Hábitos diários

- Valem para **todos os dias** (sem dias específicos da semana).
- Aparecem na Home do dia.
- Marcar como feito cria um registro de conclusão para aquela data.
- Desmarcar remove a conclusão daquela data.
- O hábito continua existindo nos dias seguintes; não há reset físico.

### 5.2 Hábitos semanais

- Possuem uma meta de quantidade de conclusões por semana.
- A Home mostra o progresso no formato `feito/meta`, por exemplo
  `1/3`.
- A semana começa na **segunda-feira**.
- A meta é **versionada**: cada alteração cria uma vigência
  (`valid_from`), e semanas passadas usam a meta vigente na época.
- A contagem é reiniciada **como visão** a cada nova semana; registros
  históricos permanecem.
- A meta pode ser ultrapassada.
- Marcar e desmarcar continuam sendo permitidos.
- A mesma conclusão não pode ser registrada duas vezes para o mesmo
  hábito e data.

### 5.3 Edição de hábito

- Nome e meta podem ser editados.
- A **frequência (diário ↔ semanal) é imutável**. Para mudar, cria-se
  outro hábito.

- O seletor de frequência aparece no formulário de edição, mas fica
  **desabilitado**.

### 5.4 Exclusão de hábito

O hábito não deve ser apagado fisicamente. Possui `created_at` e
`deleted_at`. O texto do modal de confirmação deve dizer que o hábito
será removido das telas, sem afirmar que o histórico é apagado.

- Para o usuário, o hábito desaparece completamente após a exclusão.
- Os dados históricos permanecem no banco para não quebrar os
  Insights.
- Um hábito excluído não aparece nas telas operacionais atuais.
- Os registros históricos do hábito continuam disponíveis para
  cálculos históricos dos Insights.

## 6. Registro de conclusão

O registro representa:

```text
hábito X foi concluído na data D
```

Regras:

- No máximo uma conclusão por hábito/data.
- Marcar e desmarcar são operações idempotentes.
- O registro pertence ao hábito e à data da conclusão.
- Registros não são apagados por "reset" diário ou semanal.
- A data deve ser tratada considerando o fuso configurado para o
  usuário.
- Armazenada como `DATE`, enviada pelo cliente, sem conversão de
  fuso. Restrição única `(habit_id, date)`.

## 7. Insights

### 7.1 Consistência

A consistência de um hábito é calculada por:

```text
dias feitos / dias em que o hábito existia e deveria ser considerado
```

Um hábito criado no meio do mês **não deve ser penalizado pelos dias
anteriores à sua existência**. Os dias considerados vão de `created_at`
até `deleted_at` (se houver) e, no período em andamento, até hoje
inclusive. Hábitos excluídos entram nos meses em que existiam.

A exclusão não apaga o histórico; portanto, os Insights históricos podem
continuar usando os registros do hábito mesmo depois de ele desaparecer
das telas operacionais.

### 7.2 Hábitos semanais

O progresso semanal usa:

```text
feitos / meta da semana
```

Exemplo:

```text
Meta: 3 vezes na semana
Quarta-feira: 1 conclusão
Resultado: 1/3
```

A nova semana inicia uma nova contagem de progresso, sem apagar
registros da semana anterior.

A consistência de um hábito semanal é:

```text
semanas com meta batida / semanas em que o hábito existia
```

usando a meta vigente em cada semana.

### 7.3 Ritmo

Ritmo diário, combinando hábitos diários e semanais:

```text
numerador   = conclusões do dia
denominador = hábitos diários existentes no dia
              + soma de (meta/7) dos hábitos semanais existentes
```

O valor exibido é limitado a 100%. Como o denominador pode ser
fracionário, o ritmo é exibido em **percentual**; as frações inteiras
mostradas no protótipo (ex.: `18/23`) são ilustrativas. O ritmo semanal
(barras S1 a S4) e o ritmo médio do mês agregam os dias do período.

### 7.4 Grade de hábitos por dia (Insights)

Cada célula (hábito × dia) tem um estado:

- **Concluído:** existe registro de conclusão.
- **Descanso/futuro:** dia futuro, ou dia sem conclusão de hábito
  semanal; círculo vazio neutro.
- **Não realizado:** dia passado sem conclusão, **somente para hábito
  diário**.
- **Em aberto:** hoje, sem conclusão ainda.

Hábito semanal nunca tem "não realizado" por dia. O anel de consistência
dele usa semanas com meta batida (seção 7.2).

### 7.5 Streak

**Streak foi removida.** Não implementar streak.

## 8. Entregas e eventos

### Entrega

Campos: título, data limite (`DATE`), horário limite opcional,
disciplina/projeto (texto livre, opcional), nota opcional e estado de
conclusão. Entregas atrasadas continuam visíveis na Home, marcadas como
atrasadas, até serem feitas.

### Evento

Campos: título, **categoria**, data, início, término, local/sala
opcional e flag de dia inteiro (horários em `TIMESTAMPTZ`, UTC). Sem
recorrência e sem estado feito/não feito.

Categorias (enum fixo): Acadêmico, Avaliação/Prova, Saúde/Rotina,
Pessoal. A cor do evento vem da categoria, não de urgência.

O Calendar combina os dois tipos apenas na apresentação.

A Home mostra entregas e eventos dos **próximos 4 dias**.

A cor de urgência é aplicada somente a entregas, nunca a eventos.

## 9. Backend

Arquitetura:

```text
Controller (HTTP)
        ↓
Service (regras de negócio)
        ↓
Repository (persistência)
```

Cada camada deve falar apenas com a camada imediatamente inferior.

Organização por funcionalidade:

```text
com.ensora
├─ auth/        login, JWT
├─ habit/       hábitos + registros de conclusão + metas semanais
├─ delivery/    entregas
├─ event/       eventos
├─ home/        leitura agregada da Home (orquestra outros Services)
├─ calendar/    leitura que reúne delivery + event
├─ insights/    cálculos de ritmo e consistência
└─ shared/      erros, configuração e segurança compartilhada
```

Módulos de leitura agregada (`home/`, `calendar/`, `insights/`) chamam
os **Services** de outros módulos, nunca seus repositórios.

Todas as tabelas de domínio possuem `user_id`, e o filtro por dono é
feito no `Service`. O schema é versionado com **Flyway**
(`ddl-auto=validate`).

Exemplo:

```text
habit/
├─ Controller
├─ Service
├─ Repository
├─ entidades
└─ dto/
   ├─ Request
   └─ Response
```

### Onde ficam as regras

- Dono do hábito pode marcá-lo → `Service`.
- Mesmo dia não pode gerar conclusão duplicada → `Service` + restrição
  adequada no banco.
- Janela de 4 dias → regra de consulta do `Service/Repository`.
- Cálculos de Insights → módulo `insights/`.
- Soft delete → regra de domínio no `Service`.

## 10. Autenticação

- Conta com **e-mail e senha**. E-mail normalizado em minúsculas com
  índice único; senha com BCrypt (mínimo de 8 caracteres). Erro de
  login genérico, sem revelar se o e-mail existe. Limite simples de
  tentativas de login por IP.
- **Access token (JWT, ~15 min)** devolvido no corpo da resposta e
  mantido **somente em memória** no frontend. Enviado no header
  `Authorization`.
- **Refresh token opaco (~30 dias)** em cookie
  `HttpOnly; Secure; SameSite=Strict; Path=/auth`. O banco guarda só o
  hash (`refresh_token`: `user_id`, `token_hash`, `expires_at`,
  `revoked_at`).
- `POST /auth/refresh` rotaciona o refresh token a cada uso. Reuso de
  um token já usado revoga todas as sessões do usuário. O logout revoga
  o token no servidor.
- Ao abrir ou recarregar o PWA, o frontend chama `/auth/refresh` para
  obter o access token. O renovo automático após 401 fica no cliente
  HTTP em `api/`.
- **Nada de token em localStorage/sessionStorage.**
- Frontend e API ficam no **mesmo site** via domínio próprio
  (`app.<dominio>` na Vercel e `api.<dominio>` no Render), para o
  cookie ser first-party. CORS com origem fixa e `allowCredentials`.
- Recuperação de senha e verificação de e-mail ficam fora da v1.
- A implementação seguirá Spring Security.
- `POST /auth/register` existe, mas só funciona com
  `REGISTRATION_ENABLED=true` (padrão `false`). A conta é criada uma
  vez e a flag é desligada em produção.
- Inicialmente o sistema será usado por um único usuário.
- A solução deve continuar permitindo evolução para múltiplos
  usuários.
- Essa decisão poderá ser revista antes de uma versão
  multiusuário/produtiva.

## 11. Datas e fuso horário

- Inicialmente será usado o **fuso horário do usuário atual**.
- O sistema não deve espalhar lógica de "hoje" pelo frontend/backend.
- A data relevante para consultas deve ser explícita nas requisições
  quando necessário.
- Se o sistema for expandido para vários usuários, o fuso deverá
  passar a ser uma configuração por usuário.

## 12. Frontend

Estrutura inicial:

```text
src/
├─ app/               rotas, providers, QueryClient
├─ components/
│  ├─ ui/             componentes reutilizáveis
│  └─ layout/         AppShell, BottomNav, Sidebar
├─ features/
│  ├─ home/
│  ├─ habits/
│  ├─ calendar/
│  └─ insights/
├─ api/                cliente HTTP e tipos das respostas
└─ lib/                datas e utilitários
```

Regras:

- Componentes não chamam a API diretamente.
- Componentes usam hooks de dados da própria feature.
- Hooks de dados usam TanStack Query.
- Marcar hábito deve usar atualização otimista.
- `AppShell` troca BottomNav no mobile por Sidebar no desktop.
- Insights e Calendar devem compartilhar o hook de dados quando a
  fonte de dados for a mesma.
- Criar e editar hábito usam o mesmo formulário em modos diferentes.
- Bottom sheets (novo/editar hábito, gerenciar hábitos, novo registro)
  e o modal de confirmação são componentes de `components/ui`.
- Rótulos de navegação seguem o Figma: Today, Insights, Calendar e
  Settings (engrenagem); seções da Home: Today e Upcoming.
- Tokens de design (cores, tipografia, espaçamento) serão extraídos do
  Figma para variáveis CSS globais antes das telas.

## 13. Inventário de telas

Fonte: protótipos do Figma (arquivo "Ensora", página "mockups").

### Home

Mobile: - cabeçalho com data (`TODAY · 26 SET`) e engrenagem de
Settings; - lista de hábitos do dia com checkbox (semanais mostram o
contador `feito/meta`, ex.: `1/3`); - botões Novo Hábito e Editar
Hábitos; - Upcoming: entregas e prazos (com botão Concluir e "vence em N
dias") e eventos e compromissos (data, "em N dias", horário).

Desktop: - Sidebar (Today, Insights, Calendar, Settings); - coluna Today
com hábitos; - coluna Upcoming com entregas (checkbox) e eventos.

Ações: - marcar/desmarcar hábito; - criar hábito (bottom sheet:
nome, frequência diário/semanal, meta semanal em stepper); - abrir
Gerenciar Hábitos (editar/excluir); - marcar entrega como feita;

- visualizar eventos próximos.

### Habits (bottom sheet)

Não é uma tela. "Gerenciar Hábitos" é um bottom sheet aberto pela Home,
com lista de hábitos e ações de editar (mesmo formulário, frequência
desabilitada) e excluir (modal de confirmação).

### Insights

Desktop: - grade hábito × dia do mês com navegação entre meses
(seção 7.4); - "Consistência do mês": ritmo médio, barras semanais S1 a
S4 e anel de consistência por hábito.

Mobile: ritmo semanal (frame ainda não lido).

Ações: - navegar por períodos anteriores; - analisar histórico.

Fora da v1: "Estado diário" (humor, estresse e sono), desenhado no
Figma mas adiado (decisão 11).

### Calendar

Mobile: - calendário mensal com pontos nos dias com registros;

- "Prazos de Hoje"; - lista do dia selecionado; - botão "+ Novo
  Registro" (bottom sheet com abas Evento e Entrega).

Desktop: ainda não lido (frame 214:513 do Figma).

Ações: - marcar entregas concluídas; - visualizar compromissos; - criar
evento ou entrega.

### Settings

Acessada pela engrenagem. Sem protótipo; conteúdo a definir.

### Login / Registro

Sem protótipo; a desenhar.

## 14. Contrato inicial da API

Tela Ação / dado Endpoint

---

Auth Registrar `POST /auth/register`
Auth Login `POST /auth/login`
Auth Renovar sessão `POST /auth/refresh`
Auth Sair `POST /auth/logout`
Home Carregar tela `GET /home?date=AAAA-MM-DD`
Habits Listar hábitos `GET /habits`
Home Marcar hábito `PUT /habits/{id}/completions/{data}`
Home Desmarcar hábito `DELETE /habits/{id}/completions/{data}`
Home Criar hábito `POST /habits`
Home Editar hábito `PUT /habits/{id}`
Home Excluir hábito `DELETE /habits/{id}`
Home / Calendar Marcar entrega `PATCH /deliveries/{id}`
Calendar Carregar intervalo `GET /calendar?from=...&to=...`
Calendar Criar evento `POST /events`
Calendar Editar/excluir evento `PUT/DELETE /events/{id}`
Calendar Criar entrega `POST /deliveries`
Calendar Editar/excluir entrega `PUT/DELETE /deliveries/{id}`
Insights Ritmo `GET /insights/rhythm?from=...&to=...`
Insights Consistência `GET /insights/consistency?month=AAAA-MM`

Princípios: - quando uma data for necessária para uma consulta, o
frontend a envia explicitamente; - operações de marcar/desmarcar são
idempotentes; - endpoints de leitura podem entregar dados agregados para
a tela; - operações de escrita seguem REST.

## 15. Decisões atuais

\# Decisão

---

1 PWA responsiva em vez de app nativo
2 Spring Boot no backend
3 CSS Modules, sem Tailwind
4 Sem notificação push nesta versão
5 Lista simples de hábitos, sem categorias (categorias existem só
em eventos)
6 Soft delete de hábitos, preservando histórico
7 Entrega e evento são entidades separadas
8 Streak removida
9 Consistência considera somente o período em que o hábito existia
10 Hábito semanal usa `feitos/meta` por semana
11 Estado diário (humor/estresse/sono) fora da primeira versão
12 Fuso inicialmente único; futuramente por usuário
13 Access JWT em memória + refresh token rotativo em cookie
httpOnly; sem localStorage (substitui a decisão anterior)
14 Uso pessoal primeiro; arquitetura preparada para evolução
15 Semana começa na segunda-feira
16 Frequência do hábito imutável; meta semanal versionada
17 Datas: `DATE` (conclusão, prazo) e `TIMESTAMPTZ` (evento)
18 `user_id` em todas as tabelas de domínio desde o início
19 Flyway para migrations
20 Registro protegido pela flag `REGISTRATION_ENABLED`
21 Módulos `home/`, `calendar/`, `insights/` orquestram Services
22 Cold start: tela de carregando + ping externo de keep-alive
23 PWA: instalável, shell em cache, dados somente online
24 Ritmo híbrido: denominador = diários + soma de meta/7
25 Entrega com horário opcional e disciplina em texto livre
26 Evento com categoria fixa (enum de 4 valores) e local opcional
27 Grade de Insights: "não realizado" só para hábito diário
28 Habits é bottom sheet; Estado diário adiado para depois da v1

## 16. Pontos ainda em aberto

1.  Modelagem detalhada das tabelas e relacionamentos.
2.  Definição final dos campos de cada entidade.
3.  "Pontos" não aparece em nenhum protótipo; considerar fora do
    produto.
4.  Telas de Login/Registro e Settings (sem protótipo).
5.  Frames do Figma ainda não lidos: Insights mobile (100:366),
    Calendar desktop (214:513) e frames 200:260 e 211:1703.
6.  Extrair tokens de design do Figma.
7.  Corrigir no protótipo: "Excluir Ritual?" e histórico "apagado"
    (modal de exclusão), e botão "Inserir Evento" no sheet de Entrega.
8.  Registrar o domínio próprio e configurar subdomínios na Vercel e no
    Render; decidir quando entra recuperação de senha.
9.  Confirmar limites atuais do Render/Neon (horas gratuitas, termos)
    antes de depender do keep-alive.

## 17. Ordem de implementação

Começar por uma fatia vertical pequena:

1.  Backend base + banco
2.  Login + JWT
3.  Criar hábito
4.  Marcar/desmarcar hábito
5.  Home
6.  Entregas
7.  Eventos
8.  Calendar
9.  Insights

A cada fatia: - entender o requisito; - analisar a arquitetura
existente; - planejar; - implementar; - validar; - revisar antes de
avançar.
