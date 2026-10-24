# Um único serviço: o build do front é copiado para o jar, e o Spring serve a SPA e a API na mesma origem.

# 1. Front: a API fica em /api, na mesma origem da SPA (sem CORS e com cookie first-party).
FROM node:22-alpine AS front
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_API_URL=/api
RUN npm run build

# 2. Back: o build do front entra em static/, e o jar sai com ele dentro.
FROM eclipse-temurin:21-jdk AS back
WORKDIR /app/backend
# As dependências do Maven vêm antes do código: a camada só é refeita quando o
# pom.xml muda, e um deploy que mexe só em código não baixa tudo de novo.
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN sh ./mvnw -q dependency:go-offline
COPY backend/src src
COPY --from=front /app/frontend/dist/ ./src/main/resources/static/
RUN sh ./mvnw -q -DskipTests package

# 3. Execução: só o JRE e o jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=back /app/backend/target/backend-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination /app/extraido
# Subida mais rápida: o Render grátis tem uma fração de CPU, e a JVM gastava a maior parte do
# tempo carregando classes. Aqui o contexto sobe uma vez, sem banco (Flyway desligado e dialeto
# informado), e as classes carregadas ficam gravadas em app.jsa (AppCDS). Na execução a JVM lê o
# arquivo em vez de carregar tudo de novo. Se o arquivo não servir, a JVM sobe sem ele.
RUN DB_URL=jdbc:postgresql://localhost/x DB_USER=x DB_PASSWORD=x CORS_ORIGEM=http://localhost \
    JWT_SEGREDO=treino-treino-treino-treino-treino-12345 \
    java -XX:ArchiveClassesAtExit=/app/app.jsa -XX:TieredStopAtLevel=1 -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
    -jar /app/extraido/app.jar
EXPOSE 8080
# Render grátis tem 512 MB: heap pequeno e coletor serial para caber. O compilador C1 só
# (TieredStopAtLevel=1) deixa a subida bem mais rápida e basta para o tráfego deste app.
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-XX:TieredStopAtLevel=1", "-Xmx300m", "-XX:+UseSerialGC", "-jar", "/app/extraido/app.jar"]
