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
COPY backend/ ./
COPY --from=front /app/frontend/dist/ ./src/main/resources/static/
RUN sh ./mvnw -q -DskipTests package

# 3. Execução: só o JRE e o jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=back /app/backend/target/backend-*.jar app.jar
EXPOSE 8080
# Render grátis tem 512 MB: heap pequeno e coletor serial para caber.
ENTRYPOINT ["java", "-Xmx300m", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
