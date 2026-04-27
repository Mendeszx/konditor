# Observações Técnicas — Konditor

Pontos identificados durante análise do projeto que merecem atenção antes de ir para produção.

---

## 1. Sem rate limiting nos endpoints de autenticação

**Arquivo:** `AuthController.java` — `POST /auth/google`

O endpoint de login não possui nenhum mecanismo de limitação de requisições. Um atacante pode realizar tentativas em massa sem qualquer bloqueio.

**Impacto:** Alto — risco de abuso da cota da API do Google e DDoS na aplicação.

**Sugestão:** Adicionar `spring-boot-starter-bucket4j` ou configurar rate limiting no gateway/proxy reverso (nginx, API Gateway).

---

## 2. Categorias de ingrediente não isoladas por workspace

**Arquivo:** `IngredientCategoryJpaEntity` (infra/jpa/entity)

A entidade de categoria de ingrediente não possui coluna `workspace_id`. As categorias são tratadas como globais, mas as queries de listagem filtram por workspace — se a tabela crescer com dados de múltiplos tenants sem isolamento adequado no banco, há risco de vazamento entre workspaces.

**Impacto:** Médio — potencial exposição de dados de um tenant para outro.

**Sugestão:** Adicionar `workspace_id` à tabela `ingredient_categories` e incluir o filtro em todas as queries da entidade.

---

## 3. Endpoints de lista sem paginação

**Arquivos:** `DashboardController.java` — `GET /dashboard/receitas`, endpoints de categorias em geral.

Alguns endpoints retornam listas sem paginação. À medida que o workspace cresce, essas requisições podem consumir memória excessiva.

**Impacto:** Médio — degradação de performance em workspaces com grande volume de dados.

**Sugestão:** Aplicar `PaginaResponse<T>` (já existente no projeto) nesses endpoints, seguindo o mesmo padrão de `GET /ingredientes/estoque`.

---

## 4. Gerenciamento de pedidos sem implementação

**Arquivos:** `domain/entity/Order.java`, `OrderItem.java`, `domain/enuns/OrderStatus.java` e entidades JPA correspondentes.

As entidades de pedidos estão completamente modeladas no domínio e na camada de infraestrutura, mas não há nenhum controller, use case ou service expondo essa funcionalidade.

**Impacto:** Baixo — código morto que aumenta a superfície de manutenção sem entregar valor.

**Sugestão:** Decidir se pedidos entram no roadmap imediato. Se não, remover as entidades para reduzir complexidade. Se sim, priorizar a implementação do use case.

---

## 5. JWT secret fraco no fallback de configuração

**Arquivo:** `src/main/resources/application.yaml`

```yaml
jwt:
  secret: ${JWT_SECRET:<valor_default_curto>}
```

O valor default do `JWT_SECRET` usado em desenvolvimento é curto demais para HMAC-SHA256 (requer mínimo de 32 bytes). Em um ambiente que suba sem a variável de ambiente configurada, os tokens seriam assinados com uma chave fraca.

**Impacto:** Alto em produção se `JWT_SECRET` não for configurado — tokens forjáveis.

**Sugestão:** Remover o valor default ou substituí-lo por uma string de 64+ caracteres aleatórios. Garantir que a aplicação falhe no startup se `JWT_SECRET` não estiver definido (usar `@Value` sem default ou validação via `@PostConstruct`).

---

## 6. Google Client ID exposto no código-fonte

**Arquivo:** `src/main/resources/application.yaml`

```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID:<id_hardcoded>}
```

O Client ID do projeto Google OAuth está como valor default no arquivo de configuração, que é versionado no repositório.

**Impacto:** Baixo — Client IDs do Google OAuth são semi-públicos e não são segredos críticos, mas é má prática de segurança expô-los no código-fonte.

**Sugestão:** Remover o valor default e deixar apenas `${GOOGLE_CLIENT_ID}`, forçando a configuração explícita via variável de ambiente em todos os ambientes.