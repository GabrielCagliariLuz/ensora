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
6.  **Nomes em português.** Tabelas, colunas, pacotes, endpoints e
    nomes de domínio no código ficam em português. Termos técnicos
    (Controller, Service, Repository, DTO, hook) ficam como são.

## 2. Stack

| Camada           | Tecnologia                                                  |
| ---------------- | ----------------------------------------------------------- |
| Frontend         | React + Vite + TypeScript, PWA, CSS Modules, TanStack Query |
| Backend          | Java + Spring Boot, Spring Security + JWT                   |
| Banco            | PostgreSQL local em desenvolvimento; Neon em produção       |
| Migrações        | Flyway (`ddl-auto=validate`)                                |
| Testes           | JUnit no backend; Vitest no frontend                        |
| Frontend hosting | Vercel                                                      |
| Backend hosting  | Render                                                      |

## 3. Mapa do sistema

```text
[PWA React - Vercel] -- HTTPS/JSON + JWT --> [API Spring Boot - Render] -- JDBC --> [PostgreSQL - Neon]
        |
   cache local + fila de marcações (offline)
```

- **PWA:** telas, interação, cache local dos dados e fila de marcações
  feitas offline.
- **API:** autenticação, regras de negócio e cálculos de Insights.
- **Banco:** persistência dos dados.
- Os dados ficam no servidor; cada dispositivo guarda uma cópia local
  para leitura offline e sincroniza ao reconectar (seção 13).

## 4. Conceitos de domínio

| Conceito               | Descrição                                                                                                                     |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **Usuário**            | Dono dos dados. Inicialmente haverá apenas um usuário, mas o login existe desde a v1.                                         |
| **Hábito**             | Atividade recorrente diária ou semanal. Nesta versão é somente binário: feito/não feito.                                      |
| **Registro de hábito** | Registro de que um hábito foi feito em uma determinada data. É a base da Home e dos Insights.                                 |
| **Entrega**            | Item com prazo (data e horário opcional) e estado concluída/não concluída.                                                    |
| **Evento**             | Compromisso com data, horário de início, término opcional, local opcional e cor. Apenas informativo, sem estado de conclusão. |

Entrega e evento são **entidades separadas**. O Calendar apenas reúne as
duas na leitura.

## 5. Regras de hábitos

### 5.1 Hábitos diários

- Valem para **todos os dias** (sem dias específicos da semana).
- Aparecem na Home do dia.
- Marcar como feito cria um registro de hábito para aquela data.
- Desmarcar remove o registro daquela data.
- O hábito continua existindo nos dias seguintes; não há reset físico.

### 5.2 Hábitos semanais

- Possuem uma meta de quantidade de conclusões por semana (1 a 7).
- A Home mostra o progresso no formato `feito/meta`, por exemplo
  `1/3`.
- A semana começa na **segunda-feira**.
- A meta é **versionada** (tabela `habito_meta`): cada alteração cria
  uma vigência (`vigente_desde`, sempre uma segunda-feira), e semanas
  passadas usam a meta vigente na época.
- Alterar a meta vale **a partir da semana seguinte**. A semana em
  andamento mantém a meta antiga.
- Ao criar um hábito semanal, a primeira meta vale desde a segunda-feira
  da semana de criação.
- A contagem é reiniciada **como visão** a cada nova semana; registros
  históricos permanecem.
- A meta pode ser ultrapassada.
- Marcar e desmarcar continuam sendo permitidos.
- A mesma conclusão não pode ser registrada duas vezes para o mesmo
  hábito e data.

### 5.3 Edição de hábito

- Nome e meta podem ser editados.
- O **tipo (diário ↔ semanal) é imutável**. Para mudar, cria-se
  outro hábito.
- O seletor de frequência aparece no formulário de edição, mas fica
  **desabilitado**.

### 5.4 Exclusão de hábito

O hábito não deve ser apagado fisicamente. Possui `criado_em` e
`excluido_em`. O texto do modal de confirmação deve dizer que o hábito
será removido das telas, sem afirmar que o histórico é apagado.

- Para o usuário, o hábito desaparece completamente após a exclusão.
- Os dados históricos permanecem no banco para não quebrar os
  Insights.
- Um hábito excluído não aparece nas telas operacionais atuais.
- Os registros históricos do hábito continuam disponíveis para
  cálculos históricos dos Insights.

## 6. Registro de hábito

O registro representa:

```text
hábito X foi concluído na data D
```

Regras:

- No máximo um registro por hábito/data.
- Marcar e desmarcar são operações idempotentes: marcar o que já está
  marcado ou desmarcar o que não está não gera erro.
- O registro pertence ao hábito e à data da conclusão.
- Registros não são apagados por "reset" diário ou semanal.
- A data deve ser tratada considerando o fuso configurado para o
  usuário.
- Armazenada como `DATE`, enviada pelo cliente, sem conversão de
  fuso. Restrição única `(habito_id, data)`.

## 7. Insights

### 7.1 Consistência

A consistência de um hábito é calculada por:

```text
dias feitos / dias em que o hábito existia e deveria ser considerado
```

Um hábito criado no meio do mês **não deve ser penalizado pelos dias
anteriores à sua existência**. Os dias considerados vão de `criado_em`
até `excluido_em` (se houver) e, no período em andamento, até hoje
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

- **Concluído:** existe registro de hábito.
- **Descanso/futuro:** dia futuro, ou dia sem registro de hábito
  semanal em semana ainda em andamento ou com meta batida; círculo
  vazio neutro.
- **Não realizado (vermelho):**
  - hábito diário: dia passado sem registro;
  - hábito semanal: somente depois que a semana **termina** sem bater a
    meta. Quais células ficam vermelhas está em aberto (seção 17).
- **Em aberto:** hoje, sem registro ainda.

Enquanto a semana está em andamento, o hábito semanal nunca fica
vermelho. O anel de consistência dele usa semanas com meta batida
(seção 7.2).

### 7.5 Streak

**Streak foi removida.** Não implementar streak.

## 8. Entregas e eventos

### Entrega

Campos: título, data limite (`DATE`), horário limite opcional
(`TIME`), nota opcional, estado de conclusão e data de conclusão.

- Sem horário, o app trata a entrega como vencendo às **23:59** da data
  limite (o banco guarda `NULL`).
- Concluir define `concluida = true` e `concluida_em`. Desfazer limpa
  os dois.
- Entregas atrasadas continuam visíveis na Home, marcadas como
  atrasadas, até serem concluídas.
- Uma entrega concluída continua na Home até o dia seguinte ao de
  `concluida_em` e depois some. Continua visível no Calendar.

### Evento

Campos: título, data (`DATE`), início (`TIME`), término opcional
(`TIME`), local/sala opcional e cor. Sem recorrência, sem dia inteiro
e sem estado feito/não feito.

- A cor é escolhida pelo usuário entre as cores da paleta. Não há
  categoria.
- Compromissos recorrentes (ex.: fisioterapia) são cadastrados como um
  evento por ocorrência.
- Término, quando existe, é maior que o início; o evento não atravessa
  a meia-noite.

O Calendar combina os dois tipos apenas na apresentação.

A Home mostra entregas e eventos dos **próximos 4 dias**.

A cor de urgência é aplicada somente a entregas, nunca a eventos.

## 9. Modelo de dados (MVP)

Todas as tabelas têm `id` como chave primária, do tipo `BIGINT`
identity (`GENERATED ALWAYS AS IDENTITY`). Todas as tabelas de
domínio têm `usuario_id` (FK para `usuario`), e o filtro por dono é
feito no `Service`.

### usuario

| Coluna       | Tipo        | Regras                                          |
| ------------ | ----------- | ----------------------------------------------- |
| `email`      | texto       | único; `CHECK (email = lower(email))` no banco  |
| `senha_hash` | texto       | BCrypt                                          |
| `criado_em`  | TIMESTAMPTZ | default `now()`                                 |

### refresh_token

| Coluna        | Tipo        | Regras                             |
| ------------- | ----------- | ---------------------------------- |
| `usuario_id`  | FK          |                                    |
| `token_hash`  | texto       | único; o token em si nunca é salvo |
| `expira_em`   | TIMESTAMPTZ |                                    |
| `revogado_em` | TIMESTAMPTZ | opcional                           |
| `criado_em`   | TIMESTAMPTZ | default `now()`                    |

### habito

Hábito diário e semanal ficam na mesma tabela.

| Coluna        | Tipo        | Regras                                     |
| ------------- | ----------- | ------------------------------------------ |
| `usuario_id`  | FK          |                                            |
| `nome`        | texto       | obrigatório; não vazio; até 100 caracteres |
| `tipo`        | texto       | `DIARIO` ou `SEMANAL`; imutável            |
| `criado_em`   | TIMESTAMPTZ | default `now()`                            |
| `excluido_em` | TIMESTAMPTZ | opcional (exclusão lógica)                 |

### habito_meta

Só para hábito semanal. Regra de código: todo hábito semanal tem ao
menos uma linha aqui.

| Coluna             | Tipo    | Regras                                    |
| ------------------ | ------- | ----------------------------------------- |
| `habito_id`        | FK      |                                           |
| `vezes_por_semana` | inteiro | entre 1 e 7                               |
| `vigente_desde`    | DATE    | sempre uma segunda-feira (CHECK no banco) |

Única: `(habito_id, vigente_desde)`. A meta de uma semana é a linha com
o maior `vigente_desde` menor ou igual à segunda-feira daquela semana.

### registro_habito

| Coluna      | Tipo | Regras               |
| ----------- | ---- | -------------------- |
| `habito_id` | FK   |                      |
| `data`      | DATE | enviada pelo cliente |

Única: `(habito_id, data)`. Marcar cria a linha; desmarcar apaga.

### entrega

| Coluna          | Tipo        | Regras                                 |
| --------------- | ----------- | -------------------------------------- |
| `usuario_id`    | FK          |                                        |
| `titulo`        | texto       | obrigatório, não vazio, até 150        |
| `prazo_data`    | DATE        | obrigatório                            |
| `prazo_horario` | TIME        | opcional; o app usa 23:59 quando vazio |
| `concluida`     | boolean     | default `false`                        |
| `concluida_em`  | TIMESTAMPTZ | preenchido ao concluir; nulo se `concluida = false` |
| `nota`          | texto       | opcional, até 1000                     |
| `criado_em`     | TIMESTAMPTZ | default `now()`                        |

Sem `excluido_em`: excluir uma entrega apaga a linha. Índice em
`(usuario_id, prazo_data)` para as consultas da Home e do Calendar.

### evento

| Coluna       | Tipo        | Regras                       |
| ------------ | ----------- | ---------------------------- |
| `usuario_id` | FK          |                              |
| `titulo`     | texto       | obrigatório                  |
| `data`       | DATE        | obrigatório                  |
| `inicio`     | TIME        | obrigatório                  |
| `termino`    | TIME        | opcional; maior que `inicio` |
| `local`      | texto       | opcional                     |
| `cor`        | texto curto | nome de uma cor da paleta    |
| `criado_em`  | TIMESTAMPTZ | default `now()`              |

### Restrições no banco (por migração Flyway)

- **V1 (autenticação):** `email` único e minúsculo
  (`CHECK (email = lower(email))`) e `token_hash` único.
- **V2 (hábitos):**
  - `CHECK` de `habito.tipo` em (`DIARIO`, `SEMANAL`).
  - `CHECK` de `habito.nome` não vazio, com `VARCHAR(100)`.
  - `CHECK` de `habito_meta.vezes_por_semana` entre 1 e 7.
  - `CHECK` de `habito_meta.vigente_desde` sempre uma segunda-feira.
  - Únicas `(habito_id, vigente_desde)` em `habito_meta` e
    `(habito_id, data)` em `registro_habito`.
- **V3 (entrega):**
  - `CHECK` de `entrega.titulo` não vazio, com `VARCHAR(150)`; `nota` com
    `VARCHAR(1000)`.
  - `CHECK (concluida = (concluida_em IS NOT NULL))`: concluir preenche
    os dois campos e desfazer limpa os dois, nunca um sem o outro.
  - Índice `(usuario_id, prazo_data)`.
- **V4 (evento):** escrita na vez da sua fase (a numeração é só ordem).
  Traz o `CHECK` de `evento.termino > evento.inicio` quando `termino`
  não é nulo.
- `criado_em` com default `now()` em todas as tabelas que o têm.

## 10. Backend

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
├─ auth/          login, JWT, refresh token
├─ habito/        hábitos + registros + metas semanais
├─ entrega/       entregas
├─ evento/        eventos
├─ inicio/        leitura agregada da Home (orquestra outros Services)
├─ calendario/    leitura que reúne entrega + evento
├─ insights/      cálculos de ritmo e consistência
└─ compartilhado/ erros, configuração e segurança compartilhada
```

Módulos de leitura agregada (`inicio/`, `calendario/`, `insights/`)
chamam os **Services** de outros módulos, nunca seus repositórios.

Todas as tabelas de domínio possuem `usuario_id`, e o filtro por dono é
feito no `Service`. O schema é versionado com **Flyway**
(`ddl-auto=validate`).

Exemplo:

```text
habito/
├─ HabitoController
├─ HabitoService
├─ HabitoRepository
├─ Habito            (entidade)
└─ dto/
   ├─ HabitoRequest
   └─ HabitoResponse
```

### Onde ficam as regras

- Dono do hábito pode marcá-lo → `Service`.
- Mesmo dia não pode gerar registro duplicado → `Service` + restrição
  única no banco.
- Janela de 4 dias → regra de consulta do `Service/Repository`.
- Cálculos de Insights → módulo `insights/`.
- Exclusão lógica → regra de domínio no `Service`.
- Hábito semanal com ao menos uma meta → `Service`.

### Testes

- JUnit, começando por **testes unitários dos Services**, sem banco
  (Repository simulado).
- Testes com banco real ficam para depois.

## 11. Autenticação

- Conta com **e-mail e senha**. E-mail normalizado em minúsculas com
  índice único; senha com BCrypt (mínimo de 4 caracteres, máximo de 72,
  que é o limite do BCrypt; ver decisão 40). Erro de login genérico,
  sem revelar se o e-mail existe. Limite de 5 falhas de login por IP em
  15 minutos (resposta 429), guardado em memória.
- **Access token (JWT, ~15 min)** devolvido no corpo da resposta e
  mantido **somente em memória** no frontend. Enviado no header
  `Authorization`. Assinado com HS256 (segredo em `JWT_SEGREDO`); o
  `sub` é o id do usuário. O cliente **não** deve enviar `Authorization`
  às rotas `/auth/*`: com token inválido ou expirado, o Spring responde
  401 mesmo nas rotas públicas.
- **Refresh token opaco (~30 dias)** em cookie `refresh_token`
  `HttpOnly; Secure; SameSite=Strict; Path=/auth`. São 32 bytes
  aleatórios; o banco guarda só o hash SHA-256 em hexadecimal
  (`refresh_token`: `usuario_id`, `token_hash`, `expira_em`,
  `revogado_em`), e o valor em claro só existe no cookie.
- `POST /auth/refresh` rotaciona o refresh token a cada uso. Reuso de
  um token já usado revoga todas as sessões do usuário. O logout revoga
  o token no servidor. Um token só vale uma vez: se um já revogado
  aparecer de novo (inclusive um revogado pelo logout), todas as sessões
  ativas do usuário são revogadas e a resposta é 401.
- Por isso o cliente deve manter **uma única renovação em andamento por
  vez** (por exemplo, duas abas abrindo juntas disparando duas chamadas
  com o mesmo token fariam a segunda ser tratada como reuso).
- Ao abrir ou recarregar o PWA, o frontend chama `/auth/refresh` para
  obter o access token. O renovo automático após 401 fica no cliente
  HTTP em `api/`.
- Offline, o app abre com os dados do cache local sem token. Ao
  reconectar, renova a sessão antes de enviar a fila (seção 13).
- O logout apaga o cache local e a fila do dispositivo.
- **Nada de token em localStorage/sessionStorage.**
- Frontend e API ficam no **mesmo site** via domínio próprio
  (`app.<dominio>` na Vercel e `api.<dominio>` no Render), para o
  cookie ser first-party. CORS com origem fixa e `allowCredentials`.
- Recuperação de senha e verificação de e-mail ficam fora da v1.
- A implementação seguirá Spring Security.
- `POST /auth/registrar` existe, mas só funciona com
  `REGISTRATION_ENABLED=true` (padrão `false`). A conta é criada uma
  vez e a flag é desligada em produção.
- Inicialmente o sistema será usado por um único usuário.
- A solução deve continuar permitindo evolução para múltiplos
  usuários.
- Essa decisão poderá ser revista antes de uma versão
  multiusuário/produtiva.

## 12. Datas e fuso horário

- Inicialmente será usado o **fuso horário do usuário atual**.
- O sistema não deve espalhar lógica de "hoje" pelo frontend/backend.
- A data relevante para consultas deve ser explícita nas requisições
  quando necessário.
- Datas de calendário usam `DATE` (registro de hábito, prazo de
  entrega, data do evento). Horários usam `TIME` sem fuso (horário de
  entrega, início e término do evento), interpretados no fuso do
  usuário.
- Momentos técnicos usam `TIMESTAMPTZ` (`criado_em`, `excluido_em`,
  `concluida_em`, expiração de token).
- Se o sistema for expandido para vários usuários, o fuso deverá
  passar a ser uma configuração por usuário.

## 13. Frontend

Estrutura inicial:

```text
src/
├─ app/               rotas, providers, QueryClient
├─ components/
│  ├─ ui/             componentes reutilizáveis
│  └─ layout/         AppShell, BottomNav, Sidebar
├─ features/
│  ├─ auth/
│  ├─ inicio/
│  ├─ habitos/
│  ├─ calendario/
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

### Offline

- PWA instalável, com o shell do app em cache.
- Leitura a partir de um cache local dos dados: sem conexão, as telas
  mostram o último estado conhecido.
- Marcar e desmarcar hábito funcionam offline: a operação entra numa
  fila local e é enviada ao reconectar. Como as operações são
  idempotentes, a fila guarda só o último estado de cada
  `(hábito, data)` ("último estado vence").
- Criar ou editar hábito, entrega e evento exige conexão na v1. Sem
  conexão, essas ações ficam desabilitadas com um aviso.
- Detalhes técnicos (onde persistir o cache e a fila, como reenviar)
  estão em aberto (seção 17).

## 14. Inventário de telas

Fonte: protótipos do Figma (arquivo "Ensora", página "mockups").

### Home

Mobile:

- cabeçalho com data (`TODAY · 26 SET`) e engrenagem de Settings;
- lista de hábitos do dia com checkbox (semanais mostram o contador
  `feito/meta`, ex.: `1/3`);
- botões Novo Hábito e Editar Hábitos;
- Upcoming: entregas e prazos (com botão Concluir e "vence em N dias")
  e eventos e compromissos (data, "em N dias", horário).

Desktop:

- Sidebar (Today, Insights, Calendar, Settings);
- coluna Today com hábitos;
- coluna Upcoming com entregas (checkbox) e eventos.

Ações:

- marcar/desmarcar hábito;
- criar hábito (bottom sheet: nome, frequência diário/semanal, meta
  semanal em stepper);
- abrir Gerenciar Hábitos (editar/excluir);
- marcar entrega como concluída;
- visualizar eventos próximos.

### Habits (bottom sheet)

Não é uma tela. "Gerenciar Hábitos" é um bottom sheet aberto pela Home,
com lista de hábitos e ações de editar (mesmo formulário, frequência
desabilitada) e excluir (modal de confirmação).

### Insights

Desktop:

- grade hábito × dia do mês com navegação entre meses (seção 7.4);
- "Consistência do mês": ritmo médio, barras semanais S1 a S4 e anel
  de consistência por hábito.

Mobile: ritmo semanal (frame ainda não lido).

Ações:

- navegar por períodos anteriores;
- analisar histórico.

Fora da v1: "Estado diário" (humor, estresse e sono), desenhado no
Figma mas adiado (decisão 11).

### Calendar

Mobile:

- calendário mensal com pontos nos dias com registros;
- "Prazos de Hoje";
- lista do dia selecionado;
- botão "+ Novo Registro" (bottom sheet com abas Evento e Entrega).

Desktop:

- barra lateral fixa à esquerda com a marca, Today, Insights, Calendar
  (ativo) e Settings;
- título "Calendar" e, à direita, navegação do mês (anterior, mês e ano,
  próximo), botão "Hoje" e botão "+ Novo Registro";
- grade mensal de domingo a sábado, com o número do dia em cada célula;
- dias de outros meses aparecem esmaecidos, e o dia de hoje fica
  destacado com um selo escuro;
- os registros aparecem dentro da própria célula do dia, como etiquetas:
  - evento: horário de início e título, na cor escolhida;
  - entrega: caixa de seleção e título; a prova ou entrega com prazo
    crítico usa destaque vermelho; entrega concluída aparece esmaecida
    e riscada;
- o título longo é cortado na etiqueta.

Ações:

- marcar entregas concluídas;
- visualizar compromissos;
- criar evento ou entrega.

### Settings

Acessada pela engrenagem. Sem protótipo; conteúdo a definir.

### Login / Registro

Sem protótipo; a desenhar.

## 15. Contrato inicial da API

| Tela            | Ação / dado            | Endpoint                                 |
| --------------- | ---------------------- | ---------------------------------------- |
| Auth            | Registrar              | `POST /auth/registrar`                   |
| Auth            | Login                  | `POST /auth/login`                       |
| Auth            | Renovar sessão         | `POST /auth/refresh`                     |
| Auth            | Sair                   | `POST /auth/logout`                      |
| Home            | Carregar tela          | `GET /inicio?data=AAAA-MM-DD`            |
| Habits          | Listar hábitos         | `GET /habitos`                           |
| Home            | Marcar hábito          | `PUT /habitos/{id}/registros/{data}`     |
| Home            | Desmarcar hábito       | `DELETE /habitos/{id}/registros/{data}`  |
| Home            | Criar hábito           | `POST /habitos`                          |
| Home            | Editar hábito          | `PUT /habitos/{id}`                      |
| Home            | Excluir hábito         | `DELETE /habitos/{id}`                   |
| Home / Calendar | Concluir entrega       | `PATCH /entregas/{id}`                   |
| Calendar        | Carregar intervalo     | `GET /calendario?de=...&ate=...`         |
| Calendar        | Criar evento           | `POST /eventos`                          |
| Calendar        | Editar/excluir evento  | `PUT/DELETE /eventos/{id}`               |
| Calendar        | Criar entrega          | `POST /entregas`                         |
| Calendar        | Editar/excluir entrega | `PUT/DELETE /entregas/{id}`              |
| Insights        | Ritmo                  | `GET /insights/ritmo?de=...&ate=...`     |
| Insights        | Consistência           | `GET /insights/consistencia?mes=AAAA-MM` |

Princípios:

- quando uma data for necessária para uma consulta, o frontend a envia
  explicitamente;
- operações de marcar/desmarcar são idempotentes (base da fila
  offline);
- endpoints de leitura podem entregar dados agregados para a tela;
- operações de escrita seguem REST.

### Contratos de autenticação

`POST /auth/registrar` (só com `REGISTRATION_ENABLED=true`):

```json
// requisição
{ "email": "pessoa@exemplo.com", "senha": "1234" }
// resposta 201
{ "id": 1, "email": "pessoa@exemplo.com" }
```

`POST /auth/login`:

```json
// requisição
{ "email": "pessoa@exemplo.com", "senha": "1234" }
// resposta 200 (o refresh token vai no cabeçalho Set-Cookie, não no corpo)
{ "tokenAcesso": "<jwt>", "expiraEmSegundos": 900 }
```

`POST /auth/refresh` (sem corpo; usa o cookie `refresh_token`):

```json
// resposta 200: mesmo formato do login, com um novo Set-Cookie
{ "tokenAcesso": "<jwt>", "expiraEmSegundos": 900 }
// sem cookie, token desconhecido, expirado ou revogado: 401 "Sessão inválida"
```

`POST /auth/logout` (sem corpo; usa o cookie `refresh_token`): responde
`204` e um `Set-Cookie` com `Max-Age=0` (mesmos `Path`, `Secure`,
`HttpOnly` e `SameSite` do cookie original, para o navegador apagá-lo).
É idempotente: sem cookie ou com token já revogado, também responde 204.
Revoga só o token desta sessão.

### Contratos de hábitos

`POST /habitos` (exige token):

```json
// requisição: hábito semanal (meta e data obrigatórias)
{ "nome": "Academia", "tipo": "SEMANAL", "vezesPorSemana": 3, "data": "2026-10-21" }
// requisição: hábito diário (sem meta; a data é ignorada)
{ "nome": "Beber água", "tipo": "DIARIO", "data": "2026-10-21" }
// resposta 201 (no diário, vezesPorSemana não aparece)
{ "id": 4, "nome": "Academia", "tipo": "SEMANAL", "vezesPorSemana": 3 }
```

- `nome`: obrigatório, não vazio, até 100 caracteres (gravado sem espaços
  nas pontas). Nomes repetidos são permitidos.
- `data`: o dia de hoje no fuso do usuário, lido pelo front do relógio do
  aparelho (o usuário não digita). Define a segunda-feira da primeira
  vigência da meta. O servidor recusa uma `data` a mais de 2 dias do
  relógio dele; esse limite é só uma checagem, não a fonte de "hoje".
- `vezesPorSemana`: de 1 a 7; obrigatório no semanal e proibido no diário.

`GET /habitos` (exige token) devolve a lista do usuário do token, sem os
excluídos, na ordem de criação, no mesmo formato da resposta acima. Para o
semanal, `vezesPorSemana` é a meta **mais recente** (a configurada). A meta
da semana em andamento, usada no contador `1/3` da Home, vem do
`GET /inicio`, que recebe a data.

`PUT /habitos/{id}` (exige token) usa o **mesmo corpo** do `POST`, porque
criar e editar usam o mesmo formulário; responde `200` com o hábito no
mesmo formato. O front manda o `tipo` que o hábito já tem:

- `tipo` diferente do gravado: `400` no campo `tipo` (o tipo é imutável).
- `nome`: troca o nome do hábito.
- Hábito semanal: a meta nova vale a partir da **próxima segunda-feira**
  em relação à `data` enviada. Se já existe uma vigência nessa segunda,
  ela é substituída; se o valor não muda em relação ao que já vale,
  nenhuma vigência nova é criada. A semana em andamento mantém a meta
  antiga.
- Hábito diário: `vezesPorSemana` continua proibida.

`DELETE /habitos/{id}` (exige token) responde `204` e só preenche
`excluido_em`: o hábito, as metas e os registros ficam no banco para os
Insights.

Hábito de outro usuário, inexistente ou já excluído responde sempre
`404`, nos dois endpoints.

`PUT /habitos/{id}/registros/{data}` marca o hábito como feito na data, e
`DELETE /habitos/{id}/registros/{data}` desmarca. A `{data}` é
`AAAA-MM-DD` (a do dia a que o registro se refere, no fuso do usuário).
Os dois exigem token e respondem `204` sem corpo.

- **Idempotentes:** marcar o que já está marcado e desmarcar o que não
  está marcado também respondem `204`, sem erro e sem duplicar o registro.
  É a base da fila offline (seção 13).
- Hábito de outro usuário, inexistente ou excluído: `404`.
- Marcar uma data futura (a mais de 2 dias do relógio do servidor): `400`
  no campo `data`. Datas passadas não têm limite.
- `data` fora do formato ou `id` não numérico: `400 REQUISICAO_INVALIDA`.

`GET /inicio?data=AAAA-MM-DD` (exige token) carrega a Home. A `data` é o
dia de hoje no fuso do usuário, lido pelo front do relógio do aparelho, e
é obrigatória. O servidor recusa uma `data` a mais de 2 dias do relógio
dele (`400` no campo `data`); `data` ausente ou fora do formato dá
`400 REQUISICAO_INVALIDA`.

```json
// resposta 200
{
  "data": "2026-10-21",
  "habitos": [
    { "id": 1, "nome": "Ler", "tipo": "DIARIO", "feito": true },
    { "id": 4, "nome": "Academia", "tipo": "SEMANAL", "feito": false, "feitos": 1, "meta": 3 }
  ]
}
```

- `habitos`: os do usuário do token, sem os excluídos, na ordem de
  criação. `feito` diz se há registro na `data`.
- `feitos` e `meta` só existem no hábito semanal (no diário não aparecem).
  `feitos` conta os registros de segunda a domingo da semana da `data`.
  `meta` é a que **valia naquela semana** (a vigência de maior
  `vigente_desde` menor ou igual à segunda dela), não a mais recente: uma
  meta alterada nesta semana só aparece no `/inicio` da semana seguinte.
- Hábito semanal sem meta vigente na semana da `data` (só se a `data` for
  anterior à criação dele) não aparece.
- O objeto na raiz é para crescer: o Upcoming (entregas e eventos) entra
  depois como `upcoming`, sem quebrar quem já lê `habitos`.

`POST /entregas` (exige token) cria uma entrega do usuário do token e
responde `201`. `PUT /entregas/{id}` usa o **mesmo corpo** e responde `200`
com a entrega no mesmo formato.

```json
// corpo do POST e do PUT
{ "titulo": "Trabalho de Cálculo", "prazoData": "2026-11-05", "prazoHorario": "14:30", "nota": "Cap. 1 a 4" }

// resposta
{ "id": 3, "titulo": "Trabalho de Cálculo", "prazoData": "2026-11-05", "prazoHorario": "14:30:00",
  "concluida": false, "nota": "Cap. 1 a 4" }
```

- `titulo`: obrigatório, não vazio, até 150 caracteres (gravado sem
  espaços nas pontas).
- `prazoData`: obrigatória, `AAAA-MM-DD`. Data passada é aceita (dá para
  cadastrar algo já atrasado); não há regra de "hoje" aqui.
- `prazoHorario`: opcional. Entra como `HH:mm` e **sai como `HH:mm:ss`**.
  Ausente quer dizer "sem horário": o app trata como 23:59.
- `nota`: opcional, até 1000 caracteres; texto só com espaços vale como
  ausente.
- `prazoHorario`, `nota` e `concluidaEm` só aparecem na resposta quando
  existem.
- O `PUT` troca título, prazo e nota e **não altera a conclusão**: editar
  uma entrega concluída a mantém concluída.

`PATCH /entregas/{id}` (exige token) concluir ou desfazer, com corpo
`{ "concluida": true }` ou `{ "concluida": false }` (obrigatório). Responde
`200` com a entrega.

- `true`: define `concluida = true` e `concluidaEm` com o instante do
  **relógio do servidor** (UTC, ex.: `2026-10-21T15:04:05Z`).
- `false`: define `concluida = false` e remove `concluidaEm`.
- **Idempotente:** concluir o que já está concluído mantém o
  `concluidaEm` original; desfazer o que não está concluído não faz nada.

`DELETE /entregas/{id}` (exige token) apaga a entrega e responde `204`
(exclusão física, decisão 49).

Em todos os endpoints de entrega, a entrega de outro usuário ou
inexistente responde `404`; `id` não numérico ou corpo malformado
(inclusive horário fora do formato) é `400 REQUISICAO_INVALIDA`; campo
inválido é `400 VALIDACAO` com o campo em `campos`.

### Formato de erro

Toda falha da API sai no mesmo formato (`ErroResposta`), gerado pelo
`ManipuladorDeErros`:

```json
{
  "status": 400,
  "erro": "VALIDACAO",
  "mensagem": "Dados inválidos",
  "caminho": "/habitos",
  "campos": [{ "campo": "nome", "mensagem": "não pode estar em branco" }]
}
```

| Status | `erro`                | Quando                                      |
| ------ | --------------------- | ------------------------------------------- |
| 400    | `VALIDACAO`           | corpo ou parâmetro inválido (traz `campos`) |
| 400    | `REQUISICAO_INVALIDA` | JSON malformado ou ausente; parâmetro da URL com tipo errado (ex.: id não numérico) |
| 401    | `NAO_AUTENTICADO`     | sem token válido                            |
| 403    | `ACESSO_NEGADO`       | sem permissão ou cadastro desabilitado      |
| 404    | `NAO_ENCONTRADO`      | recurso ou rota inexistente                 |
| 409    | `CONFLITO`            | conflito de dados (ex.: e-mail já cadastrado) |
| 429    | `LIMITE_EXCEDIDO`     | muitas falhas de login do mesmo IP          |
| 500    | `ERRO_INTERNO`        | falha inesperada (detalhe só no log)        |

`campos` só aparece no 400 de `VALIDACAO`. Exceção: a rejeição de CORS
(preflight de origem não permitida) é feita pelo filtro de CORS antes
do `ManipuladorDeErros` e devolve `403` com texto puro; o navegador já
bloqueia a chamada, então o front não depende desse corpo.

## 16. Decisões atuais

| #   | Decisão                                                                                                                                                                                      |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | PWA responsiva em vez de app nativo                                                                                                                                                          |
| 2   | Spring Boot no backend                                                                                                                                                                       |
| 3   | CSS Modules, sem Tailwind                                                                                                                                                                    |
| 4   | Sem notificação push nesta versão                                                                                                                                                            |
| 5   | Lista simples de hábitos, sem categorias (nem em hábitos, nem em eventos)                                                                                                                    |
| 6   | Soft delete de hábitos, preservando histórico                                                                                                                                                |
| 7   | Entrega e evento são entidades separadas                                                                                                                                                     |
| 8   | Streak removida                                                                                                                                                                              |
| 9   | Consistência considera somente o período em que o hábito existia                                                                                                                             |
| 10  | Hábito semanal usa `feitos/meta` por semana                                                                                                                                                  |
| 11  | Estado diário (humor/estresse/sono) fora da primeira versão                                                                                                                                  |
| 12  | Fuso inicialmente único; futuramente por usuário                                                                                                                                             |
| 13  | Access JWT em memória + refresh token rotativo em cookie httpOnly; sem localStorage                                                                                                          |
| 14  | Uso pessoal primeiro; arquitetura preparada para evolução                                                                                                                                    |
| 15  | Semana começa na segunda-feira                                                                                                                                                               |
| 16  | Tipo do hábito imutável; meta semanal versionada, nova meta vale da semana seguinte                                                                                                          |
| 17  | Datas em `DATE` (registro, prazo, data do evento); horários em `TIME` sem fuso (prazo, início e término do evento); `TIMESTAMPTZ` só para momentos técnicos (substitui a decisão anterior)   |
| 18  | `usuario_id` em todas as tabelas de domínio desde o início                                                                                                                                   |
| 19  | Flyway para migrations                                                                                                                                                                       |
| 20  | Registro protegido pela flag `REGISTRATION_ENABLED`                                                                                                                                          |
| 21  | Módulos `inicio/`, `calendario/`, `insights/` orquestram Services                                                                                                                            |
| 22  | Cold start: tela de carregando + ping externo de keep-alive                                                                                                                                  |
| 23  | PWA offline: instalável, shell em cache, leitura do cache local e fila de marcar/desmarcar hábito enviada ao reconectar; criar/editar exige conexão na v1 (substitui "dados somente online") |
| 24  | Ritmo híbrido: denominador = diários + soma de meta/7                                                                                                                                        |
| 25  | Entrega com horário opcional (23:59 quando vazio), sem disciplina/projeto; concluída some da Home um dia depois                                                                              |
| 26  | Evento sem categoria e sem dia inteiro; cor escolhida da paleta; término opcional; local opcional                                                                                            |
| 27  | Grade de Insights: diário fica vermelho no dia; semanal só fica vermelho quando a semana termina sem bater a meta                                                                            |
| 28  | Habits é bottom sheet; Estado diário adiado para depois da v1                                                                                                                                |
| 29  | Nomes em português em tabelas, colunas, pacotes, endpoints e domínio do código                                                                                                               |
| 30  | Hábito diário e semanal na mesma tabela; meta semanal em `habito_meta`                                                                                                                       |
| 31  | Backend organizado por funcionalidade, com Controller → Service → Repository em cada módulo                                                                                                  |
| 32  | PostgreSQL local em desenvolvimento e Neon em produção                                                                                                                                       |
| 33  | Testes: JUnit (Services unitários, sem banco) no backend; Vitest no frontend                                                                                                                 |
| 34  | `id` de todas as tabelas em `BIGINT GENERATED ALWAYS AS IDENTITY`; `UUID` descartado (a fila offline só marca/desmarca, não cria registros no cliente)                                       |
| 35  | Entidades JPA guardam FKs como `Long` (ex.: `usuarioId`), sem `@ManyToOne`; o Service filtra por `usuario_id` e busca o relacionado por id quando precisar                                   |
| 36  | Formato único de erro (`ErroResposta`, via `ManipuladorDeErros`); API stateless com CSRF desabilitado, pois o access token vai no header `Authorization` e não em cookie de sessão          |
| 37  | Access token JWT assinado com HS256 via `spring-boot-starter-oauth2-resource-server` (o Spring valida o `Bearer`); `sub` = id do usuário, validade de 15 min, segredo em `JWT_SEGREDO` (mínimo de 32 bytes, sem valor padrão) |
| 38  | Refresh token opaco de 32 bytes aleatórios; o banco guarda só o SHA-256 em hexadecimal (hash rápido, pois o token já é aleatório e a busca é pelo hash); o valor em claro só existe no cookie |
| 39  | Limite de login: 5 falhas por IP em 15 min, em memória e em código próprio, resposta 429; zera ao reiniciar a API e vale para uma instância |
| 40  | Senha com mínimo de 4 caracteres, revendo os 8 anteriores; mitigada pelo cadastro desligado em produção, pelo limite por IP e pelo uso pessoal; o máximo de 72 é o limite do BCrypt |
| 41  | Refresh token rotativo com detecção de reuso: cada token vale uma vez; token já revogado apresentado de novo revoga todas as sessões do usuário. A troca usa `UPDATE ... WHERE revogado_em IS NULL` (o banco decide quem ganha numa corrida) e `@Transactional(noRollbackFor)` para a revogação geral ser gravada mesmo com a resposta 401. Logout idempotente que revoga só o token da sessão |
| 42  | Banco reforçado em hábitos: `vigente_desde` sempre segunda-feira (`CHECK` com `ISODOW`), `nome` não vazio e com até 100 caracteres. `tipo` é um `CHECK` sobre texto (o enum fica só no Java, `TipoHabito`) e é imutável também no JPA (`updatable = false`) |
| 43  | A data de "hoje" usada por operações que dependem da semana (criar hábito semanal e, depois, mudar a meta) é enviada pelo front no corpo (`data`), lida do relógio do aparelho, com limite de 2 dias do relógio do servidor. A segunda-feira da semana vem de um único lugar (`Semana.segundaFeiraDe`, em `compartilhado/data`) |
| 44  | O Controller recebe o id do usuário logado por `@UsuarioLogado Long usuarioId` (resolvedor em `compartilhado/seguranca`, que lê o `sub` do JWT) e o repassa ao Service, que filtra por ele |
| 45  | Alteração da meta semanal: vale a partir da próxima segunda-feira em relação à `data` enviada pelo front; substitui a vigência futura se ela já existir e não cria vigência quando o valor não muda (a meta de uma semana continua sendo a vigência mais recente com `vigente_desde` menor ou igual à segunda dela) |
| 46  | Hábito de outro usuário, inexistente ou excluído responde sempre 404 (nunca 403), para não revelar se um id existe para outra pessoa. A exclusão é só lógica (`excluido_em`) e nunca apaga linhas de hábito, meta ou registro |
| 47  | Marcar hábito usa `INSERT ... ON CONFLICT (habito_id, data) DO NOTHING` e desmarcar usa `DELETE`: a idempotência é decidida pelo banco, de forma atômica, sem consulta prévia e sem depender de capturar uma exceção. Data futura (além de 2 dias do relógio do servidor) é recusada; datas passadas não têm limite |
| 48  | O `GET /inicio` calcula a meta semanal da semana da `data` (vigência de maior `vigente_desde` menor ou igual à segunda dela), não a mais recente, e conta os registros de segunda a domingo. Faz 3 consultas independentemente do número de hábitos (hábitos, metas e registros em lote). O módulo `inicio/` só orquestra: a regra mora no `HabitoService.listarDoDia` |
| 49  | A entrega garante no banco que `concluida` e `concluida_em` andem juntos (`CHECK (concluida = (concluida_em IS NOT NULL))`) e que o título não seja vazio. Entrega não tem exclusão lógica: excluir apaga a linha (diferente do hábito, que precisa preservar o histórico para os Insights) |
| 50  | Concluir entrega é `PATCH /entregas/{id}` com o estado desejado (`concluida: true` ou `false`), idempotente: concluir de novo mantém o `concluida_em` original e desfazer o que não está concluído não faz nada. `concluida_em` vem do relógio do servidor, nunca do cliente. O `PUT` da entrega não altera a conclusão |

## 17. Pontos ainda em aberto

1.  "Pontos" não aparece em nenhum protótipo; considerar fora do
    produto.
2.  Telas de Login/Registro e Settings (sem protótipo).
3.  Frames do Figma ainda não lidos: Insights mobile (100:366),
    Calendar desktop (214:513) e frames 200:260 e 211:1703.
4.  Extrair tokens de design do Figma.
5.  Corrigir no protótipo: "Excluir Ritual?" e histórico "apagado"
    (modal de exclusão), botão "Inserir Evento" no sheet de Entrega,
    campo de disciplina na entrega e categoria no evento.
6.  Registrar o domínio próprio e configurar subdomínios na Vercel e no
    Render; decidir quando entra recuperação de senha.
7.  Confirmar limites atuais do Render/Neon (horas gratuitas, termos)
    antes de depender do keep-alive.
8.  Semanal não cumprido: pintar de vermelho todos os dias não marcados
    da semana ou só um indicador por semana.
9.  Offline: onde persistir o cache e a fila (ex.: IndexedDB), quando
    reenviar a fila e como avisar falhas de envio.
10. ~~Tipo do `id` das tabelas (`BIGINT` identity ou `UUID`).~~
    Resolvido: `BIGINT` identity (decisão 34).
11. ~~Escrever a migração Flyway V2 com as restrições da seção 9.~~
    V1, V2 e V3 (entrega) prontas; as restrições de evento (V4)
    entram na vez da sua fase.
12. ~~Renomear o pacote do backend de `com.example.backend` para
    `com.ensora`.~~ Resolvido (issue #3).
13. Bibliotecas do frontend ainda indefinidas (ícones, rotas, PWA).

## 18. Ordem de implementação

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
10. Offline (cache local e fila de marcações)

A cada fatia:

- entender o requisito;
- analisar a arquitetura existente;
- planejar;
- implementar;
- testar;
- revisar antes de avançar.
