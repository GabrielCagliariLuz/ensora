# Backend do Ensora

Editado no IntelliJ. Regras gerais e arquitetura: `../CLAUDE.md` e
`../ARCHITECTURE.md` (seções Modelo de dados, Backend e Autenticação).

## Stack

- Java 21, Spring Boot 4.1.1, Maven Wrapper (Maven 3.10.0).
- Spring Web MVC, Spring Data JPA, Bean Validation, Flyway, driver
  PostgreSQL, Lombok, DevTools.
- Spring Security + JWT: a configurar.
- Banco: PostgreSQL local em desenvolvimento; Neon em produção.

## Comandos

```bash
./mvnw spring-boot:run   # sobe a API (precisa do PostgreSQL local)
./mvnw test              # roda os testes
./mvnw clean package     # gera o .jar
```

No Windows, use `mvnw.cmd`.

## Estrutura

Pacote base `com.ensora` (hoje ainda `com.example.backend`; renomear).
Uma pasta por funcionalidade, cada uma com suas camadas:

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
- Todo acesso a dados de domínio filtra por `usuario_id` no Service.

## Banco e Flyway

- Migrações em `src/main/resources/db/migration/V{n}__descricao.sql`.
- `spring.jpa.hibernate.ddl-auto=validate`: o schema vem só do Flyway.
  Configuração ainda a fazer em `application.properties`.
- Nunca edite uma migração já aplicada; crie a próxima versão.
- Credenciais do banco por variável de ambiente, nunca no repositório.

## Testes

- JUnit 5 com Mockito (vêm nos starters de teste do pom).
- Comece por testes unitários dos Services, com o Repository simulado e
  sem subir o Spring nem o banco.
- Um teste por regra: `HabitoServiceTest`, método com nome que descreve
  o caso (`marcarDuasVezesNaoDuplicaRegistro`).
- O `BackendApplicationTests` atual sobe o contexto inteiro e precisa do
  banco rodando. Testes com banco ficam para depois.
