# Backend do Ensora

Editado no IntelliJ. Regras gerais e arquitetura: `../CLAUDE.md` e
`../ARCHITECTURE.md` (seções Modelo de dados, Backend e Autenticação).

## Stack

- Java 21, Spring Boot 4.1.1, Maven Wrapper (Maven 3.10.0).
- Spring Web MVC, Spring Data JPA, Bean Validation, Flyway, driver
  PostgreSQL, Lombok, DevTools.
- Spring Security (stateless, CORS) e JWT (HS256, via
  `oauth2-resource-server`) configurados.
- Banco: PostgreSQL local em desenvolvimento; Neon em produção.

## Comandos

```bash
./mvnw spring-boot:run   # sobe a API (precisa do PostgreSQL local)
./mvnw test              # testes unitários (sem banco, sem Docker)
./mvnw verify            # unitários + integração (exige o Docker aberto)
./mvnw clean package     # gera o .jar
```

No Windows, use `mvnw.cmd`.

## Estrutura

Pacote base `com.ensora`. Uma pasta por funcionalidade, cada uma com suas camadas:

```text
com.ensora/habito/
  HabitoController   HTTP: recebe DTO, chama o Service, devolve DTO
  HabitoService      regras de negócio e filtro por usuario_id
  HabitoRepository   Spring Data JPA
  Habito             entidade
  dto/               HabitoRequest, HabitoResponse
```

## Regras

- Controller → Service → Repository. Cada camada só fala com a de baixo.
- Módulos agregados (`inicio`, `calendario`, `insights`) chamam Services
  de outros módulos, nunca Repositories.
- Entidade nunca sai do Controller; a API recebe e devolve DTOs
  (prefira `record`).
- Validação de entrada com Bean Validation nos DTOs de request.
- Todo acesso a dados de domínio filtra por `usuario_id` no Service. O
  Controller recebe `@UsuarioLogado Long usuarioId` e o repassa ao
  Service; nunca converte o token à mão.
- Operação idempotente que pode colidir com uma restrição única (ex.:
  marcar hábito) usa `INSERT ... ON CONFLICT DO NOTHING`. Capturar a
  `DataIntegrityViolationException` dentro de uma transação não serve:
  a transação já fica marcada para desfazer e a resposta vira 500.
- Recurso de outro usuário (ou excluído) é 404, nunca 403: o filtro por
  dono mora na consulta do Repository (ex.:
  `findByIdAndUsuarioIdAndExcluidoEmIsNull`) e o Service lança
  `RecursoNaoEncontradoException` quando ela volta vazia.
- Regra de validação que o Bean Validation não expressa (ex.: "semanal
  exige meta"): o Service lança `ValidacaoException(campo, mensagem)`.
- A segunda-feira de uma data vem de `Semana.segundaFeiraDe`
  (`compartilhado/data`), nunca de uma conta repetida em outro lugar.
- Entidades guardam FK como `Long` (ex.: `usuarioId`), nunca `@ManyToOne`.
  Para dados do relacionado, o Service busca pelo id.
- Erros saem sempre pelo `ManipuladorDeErros` (`compartilhado/erro`): o
  Service lança exceção (ex.: `RecursoNaoEncontradoException`) e o
  Controller nunca monta JSON de erro.
- Método transacional que grava e depois lança exceção precisa de
  `@Transactional(noRollbackFor = ...)`, senão o que foi gravado é
  desfeito (ex.: `RefreshTokenService.rotacionar` revoga as sessões e
  só então responde 401). Teste unitário com Repository simulado não
  pega isso; foi conferido à mão no banco.

## Banco e Flyway

- Migrações em `src/main/resources/db/migration/V{n}__descricao.sql`.
- `spring.jpa.hibernate.ddl-auto=validate`: o schema vem só do Flyway.
- Nunca edite uma migração já aplicada; crie a próxima versão.
- Credenciais do banco por variável de ambiente, nunca no repositório.
  O `application.properties` lê `DB_URL`, `DB_USER` e `DB_PASSWORD`, sem
  valor padrão: sem elas a app não sobe. No IntelliJ, defina em
  Run Configuration → Environment variables. Exemplo de `DB_URL`:
  `jdbc:postgresql://localhost:5432/ensora`.
- CORS: `CORS_ORIGEM` (origem exata do front, sem valor padrão). Em
  desenvolvimento: `http://localhost:5173`.
- JWT: `JWT_SEGREDO` (texto com no mínimo 32 bytes, sem valor padrão; a
  app não sobe sem ele). Trocar o valor invalida os tokens já emitidos.
- Fuso: `FUSO` (opcional, padrão `America/Sao_Paulo`). Transforma
  `criado_em` e `excluido_em` no dia do usuário nos Insights.
- Cadastro: `REGISTRATION_ENABLED` (padrão `false`). Ligue (`true`) só
  para criar a conta e desligue depois; em produção fica desligada.
- Prefixo da API: `API_PREFIXO` (padrão vazio; em produção `/api`). Com
  ele, os controllers ficam sob `/api` e a SPA é servida de `static/`. Em
  dev fica vazio. Sem barra no início, a app não sobe.
- Porta: `PORT` (padrão 8080; o Render define).
- `GET /api/saude` (ou `/saude` em dev): público e sem banco. É o health
  check do Render e o alvo do monitor de keep-alive.
- Produção: `docker build -t ensora .` na raiz. O Dockerfile gera o front
  com `VITE_API_URL=/api` e copia o `dist` para `static/` antes do jar.

## Testes

- JUnit 5 com Mockito (vêm nos starters de teste do pom).
- Comece por testes unitários dos Services, com o Repository simulado e
  sem subir o Spring nem o banco.
- Um teste por regra: `HabitoServiceTest`, método com nome que descreve
  o caso (`marcarDuasVezesNaoDuplicaRegistro`).
- Testes de integração ficam em `src/test/java/com/ensora/integracao`,
  terminam em `IT` e estendem `IntegracaoBase`: app inteira contra um
  PostgreSQL descartável (Testcontainers). Rodam só no `./mvnw verify`
  (Failsafe) e exigem o Docker aberto. Cada teste cria as próprias contas.
- O `BackendApplicationTests` fica desabilitado; o contexto real já sobe
  nos testes de integração.
