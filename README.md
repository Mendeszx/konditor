# Konditor — Backend

API REST de **inteligência financeira para confeitarias e docerias**. O Konditor ajuda o
confeiteiro a cadastrar ingredientes, montar receitas, calcular o custo real de produção e
descobrir o preço de venda ideal com base na margem desejada — tudo isolado por workspace
(multi-tenant).

> Este repositório contém **apenas o backend**. O front-end é um projeto separado
> (`konditor-web`) e consome esta API.

---

## Sumário

- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Modelo de domínio](#modelo-de-domínio)
- [Autenticação e segurança](#autenticação-e-segurança)
- [Endpoints da API](#endpoints-da-api)
- [Como rodar](#como-rodar)
- [Configuração (variáveis de ambiente)](#configuração-variáveis-de-ambiente)
- [Banco de dados](#banco-de-dados)
- [Build, formatação e testes](#build-formatação-e-testes)
- [Observabilidade](#observabilidade)
- [Roadmap / limitações conhecidas](#roadmap--limitações-conhecidas)
- [Licença](#licença)

---

## Stack

| Camada            | Tecnologia                                             |
| ----------------- | ------------------------------------------------------ |
| Linguagem         | Java 17                                                |
| Framework         | Spring Boot 4.0.5 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Banco de dados    | PostgreSQL (driver JDBC)                               |
| Autenticação      | Google Sign-In (verificação de ID Token) + JWT (jjwt 0.12.6) |
| Mapeamento        | MapStruct 1.6.3                                         |
| Boilerplate       | Lombok                                                  |
| Formatação        | Spotless + Google Java Format (aplicado no build)      |
| Build             | Maven (Maven Wrapper incluso)                          |
| Containers        | Docker + Docker Compose (API + PostgreSQL)             |

---

## Arquitetura

Arquitetura em camadas, organizada sob o pacote base `com.api.konditor`:

```
com.api.konditor
├── app                       # Camada de aplicação / entrada HTTP
│   ├── controller            # Controllers REST + request/response DTOs
│   ├── service               # Orquestração de casos de uso
│   ├── config                # SecurityConfig, ApplicationConfig, filtros JWT
│   ├── exception             # Exceptions de domínio + GlobalExceptionHandler
│   └── schedule              # Jobs agendados (limpeza de refresh tokens)
├── domain                    # Regras de negócio puras
│   ├── entity                # Entidades de domínio (User, Workspace, Product, Order...)
│   ├── enuns                 # Enums ricos (Role, Plan, OrderStatus, UnitType...)
│   └── useCase               # Interfaces de caso de uso + implementações (impl/)
└── infra                     # Detalhes de infraestrutura
    ├── jpa                   # Entidades JPA, repositórios, mappers (MapStruct)
    └── googleprovider        # Verificação de Google ID Token
```

Princípios:

- **Domain isolado**: entidades de domínio são separadas das entidades JPA (`infra.jpa.entity`),
  com mappers MapStruct fazendo a tradução entre as camadas.
- **Casos de uso** definidos como interfaces em `domain.useCase`, implementados em
  `domain.useCase.impl` e orquestrados pelos `service` da camada `app`.
- **Multi-tenant**: todo dado de negócio pertence a um `workspace`, e o `workspaceId` é resolvido
  a partir das claims do JWT (nunca de parâmetro do request).

---

## Modelo de domínio

O schema (`src/main/resources/database/schema_completo.sql`) usa o schema PostgreSQL `konditor` e a
extensão `pgcrypto` para geração de UUIDs. Principais entidades:

- **users** — usuários autenticados via Google (`google_id`, `email`, `locale`).
- **workspaces** — tenant principal; cada workspace tem um `owner`, um `plan` e uma `currency`.
- **workspace_members** — vínculo usuário↔workspace com um `role` (`owner`, `admin`, `member`).
- **plan_details** — planos comerciais (`free`, `basic`, `premium`) com limites de uso e flags de
  funcionalidades (cálculo de custo, gestão de pedidos, relatórios, auditoria, etc.).
- **ingredients** — insumos do workspace, com custo por unidade, estoque, alerta de estoque mínimo,
  categoria e código opcional.
- **ingredient_price_history** — histórico de alterações de preço dos ingredientes.
- **products** — receitas/produtos, com preço de venda, rendimento, custo calculado, preço sugerido
  e status (`rascunho` / `publicada`).
- **product_ingredients** — composição da receita (ingrediente + quantidade + unidade).
- **product_recipe_ingredients** — sub-receitas: uma receita usada como ingrediente de outra
  (receita-ingrediente + quantidade), com o custo propagado no cálculo do produto pai.
- **orders** / **order_items** — pedidos e seus itens (feature modelada; ver
  [limitações](#roadmap--limitações-conhecidas)).
- **subscriptions** — assinatura de plano por workspace.
- **audit_logs** — trilha de auditoria (`CREATE` / `UPDATE` / `DELETE`) com `data_before` /
  `data_after` em JSONB.
- **refresh_tokens** — tokens de renovação de sessão.

Tabelas de domínio auxiliar já vêm populadas via `insert` no schema: `roles`, `plan_details`,
`unit_types`, `order_statuses`, `subscription_statuses`, `audit_operations`, além de **seeds**
de unidades de medida (com conversões peso/volume/contagem), categorias de produto e categorias
de ingrediente.

Todas as tabelas de negócio usam **soft-delete** (`deleted_at`) e colunas de auditoria
(`created_at`, `updated_at`, `created_by`, `updated_by`).

---

## Autenticação e segurança

Fluxo **stateless** baseado em JWT, com login social pelo Google:

1. O front obtém um **Google ID Token** e envia para `POST /auth/google`.
2. O backend valida o ID Token junto ao Google, cria/atualiza o usuário e retorna:
   - **access token (JWT)** no corpo da resposta;
   - **refresh token** em cookie `HttpOnly` (`refresh_token`).
3. Quando o access token expira, o front chama `POST /auth/refresh` — o cookie é enviado
   automaticamente pelo browser, sem header `Authorization`.
4. `POST /auth/logout` revoga todos os refresh tokens do usuário e limpa o cookie.

Detalhes de segurança:

- Sessão **stateless** (`SessionCreationPolicy.STATELESS`), CSRF desabilitado (API REST sem estado).
- **CORS** restrito às origens de `CORS_ALLOWED_ORIGINS`, com credenciais habilitadas.
- Endpoints públicos: `POST /auth/google`, `POST /auth/refresh`, `OPTIONS /**` e
  `GET /actuator/health/**`. **Todos os demais exigem JWT válido.**
- O `workspaceId` do usuário vem das claims do JWT (`UsuarioAutenticado`) — garantindo o
  isolamento multi-tenant.
- `JWT_SECRET` é **obrigatório** (sem default): a aplicação falha ao iniciar sem ele
  (`JwtService` valida presença e tamanho mínimo de 32 caracteres).
- **Rate limiting por IP** nos endpoints públicos de auth (`AuthRateLimitFilter`): por padrão
  10 requisições/60s por IP em `/auth/google` e `/auth/refresh`; excedentes recebem `429` com
  header `Retry-After`. Limites configuráveis via `RATE_LIMIT_*`.
- Um job agendado (`TokenCleanupScheduler`) remove refresh tokens expirados diariamente
  (cron configurável via `TOKEN_CLEANUP_CRON`).

---

## Endpoints da API

Todos os endpoints exigem `Authorization: Bearer <jwt>`, exceto os marcados como **público**.

### Autenticação — `/auth`

| Método | Rota             | Descrição                                                        |
| ------ | ---------------- | ---------------------------------------------------------------- |
| POST   | `/auth/google`   | **Público.** Troca Google ID Token por JWT + cookie de refresh.  |
| POST   | `/auth/refresh`  | **Público.** Renova o access token via cookie `refresh_token`.   |
| POST   | `/auth/logout`   | Revoga todas as sessões do usuário e limpa o cookie.             |

### Onboarding — `/onboarding`

| Método | Rota           | Descrição                                                         |
| ------ | -------------- | ---------------------------------------------------------------- |
| POST   | `/onboarding`  | Cria o primeiro workspace do usuário e conclui o setup inicial.  |

### Dashboard — `/dashboard`

| Método | Rota                       | Descrição                                                   |
| ------ | -------------------------- | ----------------------------------------------------------- |
| GET    | `/dashboard/estatisticas`  | Estatísticas gerais do workspace (KPIs).                    |
| GET    | `/dashboard/receitas`      | Lista receitas em cards. Filtra por `status` (padrão `publicada`). |

### Receitas — `/receitas` e `/ingredientes`

| Método | Rota                          | Descrição                                                          |
| ------ | ----------------------------- | ----------------------------------------------------------------- |
| POST   | `/receitas`                   | Cria uma receita (rascunho por padrão). Aceita ingredientes e **sub-receitas** (`receitasComoIngredientes`). Custo/preço calculados no servidor. |
| GET    | `/receitas/{id}`              | Detalhes completos da receita, com ingredientes, sub-receitas e custos. |
| PUT    | `/receitas/{id}`              | Atualiza a receita (substitui ingredientes e sub-receitas e recalcula custos). |
| POST   | `/receitas/{id}/publicar`     | Publica a receita (`rascunho` → `publicada`).                     |
| POST   | `/receitas/calcular`          | Calcula custos e preço sugerido em tempo real, **sem persistir**. |
| GET    | `/receitas/categorias`        | Lista categorias de receita (chips de filtro).                    |
| GET    | `/ingredientes?query=`        | Autocomplete de ingredientes por nome.                            |

### Estoque / Ingredientes — `/ingredientes`

| Método | Rota                                    | Descrição                                                   |
| ------ | --------------------------------------- | ----------------------------------------------------------- |
| POST   | `/ingredientes/estoque`                 | Cadastra um ingrediente no estoque.                         |
| GET    | `/ingredientes/estoque/{id}`            | Detalhes de um ingrediente.                                 |
| GET    | `/ingredientes/estoque`                 | Lista paginada. Filtra por `categoriaId`, `pagina`, tamanho.|
| PUT    | `/ingredientes/estoque/{id}`            | Atualiza um ingrediente.                                    |
| GET    | `/ingredientes/estoque/resumo`          | Resumo agregado do estoque.                                 |
| GET    | `/ingredientes/estoque/alertas-mercado` | Itens abaixo do estoque mínimo / alertas de mercado.        |
| GET    | `/ingredientes/categorias`              | Lista categorias de ingrediente.                            |

### Unidades — `/unidades`

| Método | Rota               | Descrição                                                      |
| ------ | ------------------ | ------------------------------------------------------------- |
| GET    | `/unidades?tipo=`  | Lista unidades de medida. Filtra por `tipo` (weight/volume/unit). |

---

## Como rodar

### Opção A — Docker (recomendado, portátil)

A forma mais simples de rodar em **qualquer máquina**: sobe a API e o PostgreSQL juntos, já com o
schema aplicado automaticamente. Só requer **Docker** e **Docker Compose**.

```bash
cp .env.example .env      # opcional: ajuste os valores
docker compose up --build
```

- A API sobe em `http://localhost:8080` e o Postgres em `localhost:5432`.
- O `schema_completo.sql` é aplicado automaticamente na **primeira** subida do banco (via
  `docker-entrypoint-initdb.d`); o app só inicia depois que o banco fica saudável (`healthcheck`).
- Para parar: `docker compose down`. Para zerar o banco (recomeçar do schema): `docker compose down -v`.

Verifique com:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

> As variáveis do `.env` sobrescrevem os defaults do compose. Para login real, informe um
> `GOOGLE_CLIENT_ID` próprio; em produção, defina `JWT_SECRET` forte e `COOKIE_SECURE=true`.

### Opção B — Manual (JDK + PostgreSQL locais)

#### Pré-requisitos

- **JDK 17**
- **PostgreSQL** em execução com um banco `konditor` disponível
- Um **Google OAuth Client ID** válido (para login) — configure via `GOOGLE_CLIENT_ID`

### 1. Prepare o banco

Crie o banco e aplique o schema (o script já cria o schema `konditor`, tabelas e seeds):

```bash
createdb konditor
psql -d konditor -f src/main/resources/database/schema_completo.sql
```

> O projeto roda com `spring.jpa.hibernate.ddl-auto=validate` por padrão, ou seja, o Hibernate
> **valida** o schema mas não o cria. Aplicar o `schema_completo.sql` manualmente (ou via ferramenta de
> migração) é necessário antes de subir a aplicação.

### 2. Configure as variáveis de ambiente

No mínimo, aponte para o seu banco, defina o client ID do Google e o `JWT_SECRET` (**obrigatório**
— a aplicação falha ao iniciar sem ele). Para desenvolvimento local em HTTP, desabilite o cookie
seguro:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/konditor"
export DB_USER="postgres"
export DB_PASS="postgres"
export GOOGLE_CLIENT_ID="seu-client-id.apps.googleusercontent.com"
export JWT_SECRET="uma-chave-secreta-forte-e-aleatoria"
export COOKIE_SECURE="false"
export CORS_ALLOWED_ORIGINS="http://localhost:5500,http://127.0.0.1:5500"
```

### 3. Suba a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Verifique com:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Configuração (variáveis de ambiente)

As configurações têm defaults em `src/main/resources/application.yaml` — exceto `JWT_SECRET`,
que é obrigatória. Sobrescreva via variáveis de ambiente:

| Variável                        | Default                                   | Descrição                                                  |
| ------------------------------- | ----------------------------------------- | ---------------------------------------------------------- |
| `DB_URL`                        | `jdbc:postgresql://localhost:5432/konditor` | URL JDBC do PostgreSQL.                                   |
| `DB_USER`                       | `postgres`                                | Usuário do banco.                                          |
| `DB_PASS`                       | `postgres`                                | Senha do banco.                                            |
| `DDL_AUTO`                      | `validate`                                | Modo de DDL do Hibernate (`validate` recomendado).         |
| `GOOGLE_CLIENT_ID`              | *(default de dev)*                        | Client ID do Google OAuth usado para validar o ID Token.   |
| `JWT_SECRET`                    | *(sem default — obrigatória)*             | **A app não sobe sem ela.** Segredo de assinatura do JWT (mín. 32 chars). |
| `JWT_EXPIRATION_SECONDS`        | `3600`                                    | Validade do access token (segundos).                       |
| `JWT_REFRESH_EXPIRATION_SECONDS`| `2592000`                                 | Validade do refresh token (segundos; padrão 30 dias).      |
| `COOKIE_SECURE`                 | `true`                                    | `false` apenas em dev local (HTTP). Sempre `true` em prod. |
| `CORS_ALLOWED_ORIGINS`          | `http://127.0.0.1:5500,http://localhost:5500` | Origens permitidas (separadas por vírgula). Nunca use `*` em prod. |
| `RATE_LIMIT_ENABLED`            | `true`                                    | Habilita rate limiting por IP em `/auth/google` e `/auth/refresh`. |
| `RATE_LIMIT_MAX_REQUESTS`       | `10`                                      | Máximo de requisições por IP dentro da janela.             |
| `RATE_LIMIT_WINDOW_SECONDS`     | `60`                                      | Tamanho da janela de rate limiting (segundos).             |
| `RATE_LIMIT_TRUST_XFF`          | `false`                                   | Usa o IP do `X-Forwarded-For` (habilite só atrás de proxy confiável). |
| `TOKEN_CLEANUP_CRON`            | `0 0 3 * * *`                             | Cron de limpeza de refresh tokens expirados (03:00 UTC).   |
| `HEALTH_PROBES_ENABLED`         | `true`                                    | Habilita as probes liveness/readiness do Actuator.         |

> ⚠️ **Produção**: sempre defina `JWT_SECRET` com um valor forte, mantenha `COOKIE_SECURE=true`,
> restrinja `CORS_ALLOWED_ORIGINS` à URL exata do front e forneça um `GOOGLE_CLIENT_ID` próprio.

---

## Banco de dados

- Schema consolidado em `src/main/resources/database/schema_completo.sql` (todas as versões unificadas).
- **Flyway não está no `pom.xml`** — a aplicação apenas **valida** o schema (`ddl-auto=validate`).
  Aplique o script manualmente ou integre uma ferramenta de migração antes de subir.
- Extensão exigida: `pgcrypto` (para `gen_random_uuid()`).
- Estratégias adotadas: soft-delete via `deleted_at`, índices parciais para registros ativos,
  índices únicos case-insensitive por workspace, e trilha de auditoria em `audit_logs` (JSONB).

---

## Build, formatação e testes

```bash
# Compilar
./mvnw clean compile

# Empacotar o JAR executável
./mvnw clean package
java -jar target/konditor-0.0.1-SNAPSHOT.jar

# Rodar em modo dev
./mvnw spring-boot:run
```

- **Formatação automática**: o Spotless (Google Java Format) roda na fase `validate` do Maven e
  **reformata os fontes automaticamente** a cada build. Não é necessário formatar manualmente.
- **Testes**: a suíte de testes unitários começou pelo caso de uso de receitas
  (`ReceitaUseCaseImplTest`, cobrindo cálculo de custos, sub-receitas e validações). Rode com
  `./mvnw test`. A cobertura dos demais casos de uso ainda está em aberto — contribuições são bem-vindas.

---

## Observabilidade

Actuator habilitado com exposição mínima:

- `GET /actuator/health` — status geral (**público**; retorna `{"status":"UP"}`).
- `GET /actuator/health/liveness` e `GET /actuator/health/readiness` — probes para Kubernetes.
- Detalhes completos do health (componentes, disco, banco) são exibidos apenas para requisições
  autenticadas (`show-details: when-authorized`).

> Por segurança, apenas o endpoint `health` é exposto — `env`, `beans`, `mappings` etc.
> permanecem desabilitados.

---

## Roadmap / limitações conhecidas

- **Pedidos** (`orders` / `order_items`): entidades e schema já modelados, mas **sem controller
  exposto** — feature planejada.
- **Autorização por papel**: os papéis (`owner`/`admin`/`member`) existem no schema e nas claims,
  mas ainda **não há checagem de permissão** por endpoint (todo usuário autenticado tem acesso).
- **Migrations**: sem Flyway/Liquibase no build; o schema é aplicado manualmente.
- **Testes**: cobertura ainda parcial — há testes para o caso de uso de receitas
  (`ReceitaUseCaseImplTest`), mas os demais casos de uso seguem sem cobertura automatizada.

---

## Licença

Distribuído sob a licença **MIT**. Veja [`LICENSE`](LICENSE) para o texto completo.

Copyright © 2026 Guilherme Mendes.
