# syntax=docker/dockerfile:1

# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build: compila e empacota o JAR com Maven (não depende do mvnw,
# cujo wrapper jar não é versionado). Usa cache de dependências para builds
# incrementais mais rápidos.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Baixa as dependências primeiro (camada cacheável enquanto o pom não muda)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Compila e empacota (Spotless roda na fase validate e formata os fontes)
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime: imagem enxuta apenas com o JRE + o JAR final.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# curl usado pelo healthcheck do docker-compose
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /app/target/konditor-0.0.1-SNAPSHOT.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
