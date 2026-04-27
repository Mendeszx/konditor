# TASK — Página de Assinatura e Integração com Stripe

**Módulo:** Billing / Assinaturas  
**Prioridade:** Alta  
**Estimativa:** 8–13 dias  
**Contexto:** Konditor SaaS — Spring Boot 4.0.5, DDD, PostgreSQL, multi-tenant por workspace  
**Dependência externa:** [Stripe API v2024-12-18](https://stripe.com/docs/api) — SDK `com.stripe:stripe-java:27.x`

---

## 1. Objetivo

Implementar o sistema completo de **assinaturas e cobrança recorrente** do Konditor, integrando com o Stripe como processador de pagamentos. O sistema deve cobrir:

- Exibição da página de planos com preços mensais e anuais
- Checkout seguro via Stripe Checkout (hosted page) para novas assinaturas
- Portal do cliente via Stripe Billing Portal para gerenciamento autônomo (trocar plano, atualizar cartão, cancelar)
- Recebimento e processamento de webhooks do Stripe para manter o estado local sincronizado
- Período de trial de 14 dias para novos workspaces (plano `basic` ou `premium`)
- Lógica de grace period de 3 dias após falha de pagamento antes de suspender o acesso
- Histórico de faturas acessível pelo owner do workspace
- Painel administrativo com MRR, Churn e relatório de receita

---

## 2. Planos de Assinatura

### 2.1 Estrutura de Planos

O Konditor possui três planos. Os preços já existem na tabela `detalhes_plano` do banco de dados, mas precisam ser expandidos para suportar **ciclo anual** e **IDs dos preços no Stripe** (`stripe_price_id`).

| Plano     | Mensal    | Anual (economize 20%) | Trial | Target                              |
|-----------|-----------|-----------------------|-------|-------------------------------------|
| `free`    | Grátis    | Grátis                | —     | Doceiros iniciando, sem volume      |
| `basic`   | R$ 29,90  | R$ 287,04 (R$ 23,92/mês) | 14d | Doceiros em crescimento            |
| `premium` | R$ 69,90  | R$ 671,04 (R$ 55,92/mês) | 14d | Profissionais e pequenos negócios  |

### 2.2 Features por Plano

| Feature                        | Free  | Basic  | Premium |
|--------------------------------|-------|--------|---------|
| Workspaces                     | 1     | 1      | 3       |
| Membros por workspace          | 1     | 3      | 10      |
| Produtos (receitas)            | 10    | 50     | Ilimitado |
| Ingredientes                   | 20    | 100    | Ilimitado |
| Pedidos/mês                    | 20    | 100    | Ilimitado |
| Cálculo de custos              | ✅    | ✅     | ✅      |
| Gestão de pedidos              | ❌    | ✅     | ✅      |
| Relatórios financeiros         | ❌    | ❌     | ✅      |
| Log de auditoria               | ❌    | ❌     | ✅      |
| Acesso à API                   | ❌    | ❌     | ✅      |
| Suporte prioritário            | ❌    | ❌     | ✅      |
| Exportação (CSV/PDF)           | ❌    | ❌     | ✅      |

---

## 3. Modelo de Banco de Dados

### 3.1 Alterações na tabela `detalhes_plano`

Adicionar colunas para suporte a ciclo anual e IDs no Stripe.

```sql
alter table konditor.detalhes_plano
    -- IDs dos objetos correspondentes no Stripe
    add column stripe_product_id    varchar(100),   -- ex: 'prod_xxxxx'
    add column stripe_price_mensal  varchar(100),   -- ex: 'price_xxxxx' (monthly)
    add column stripe_price_anual   varchar(100),   -- ex: 'price_xxxxx' (yearly)

    -- Preços anuais (em centavos)
    add column preco_anual_centavos  integer,        -- ex: 28704 (R$ 287,04)

    -- Controle de trial
    add column trial_dias            integer not null default 0,

    -- Destaque na página de planos
    add column destaque              boolean not null default false,
    add column badge_texto           varchar(50);    -- ex: 'Mais popular'
```

> **Observação:** O plano `free` não possui `stripe_price_id` pois não tem cobrança. A criação dos produtos e preços no Stripe Dashboard deve ser feita manualmente ou via script de seed (`StripeProductSeeder.java`) antes do deploy.

---

### 3.2 Alterações na tabela `assinaturas`

Expand da tabela existente para suportar todos os dados necessários da integração com Stripe.

```sql
alter table konditor.assinaturas
    -- Stripe Customer ID — armazenado no workspace
    -- (ver tabela espacos_trabalho após alteração)

    -- Stripe Subscription ID já existe como id_assinatura_externo

    -- Ciclo de cobrança contratado
    add column ciclo_cobranca        varchar(10) not null default 'monthly'
        constraint chk_assinaturas_ciclo check (ciclo_cobranca in ('monthly', 'yearly')),

    -- Item ID da assinatura no Stripe (necessário para fazer upgrade/downgrade)
    add column stripe_subscription_item_id varchar(100),

    -- Stripe Price ID vinculado ao plano contratado
    add column stripe_price_id       varchar(100),

    -- Cancelamento agendado (cancel_at_period_end = true no Stripe)
    add column cancelamento_agendado boolean not null default false,
    add column cancelamento_em       timestamptz,   -- quando o acesso expira após cancelamento

    -- Grace period: tempo extra após past_due antes de suspender
    add column grace_period_ate      timestamptz,

    -- Metadados do último evento Stripe processado (idempotência)
    add column ultimo_evento_stripe_id   varchar(100),
    add column ultimo_evento_stripe_em   timestamptz;
```

---

### 3.3 Alterações na tabela `espacos_trabalho`

```sql
alter table konditor.espacos_trabalho
    add column stripe_customer_id    varchar(100) unique; -- ex: 'cus_xxxxx'
```

> O `stripe_customer_id` é criado no momento em que o usuário inicia o primeiro checkout e deve ser reutilizado em todas as assinaturas futuras do workspace.

---

### 3.4 Nova tabela `faturas`

Armazena o espelho local das invoices do Stripe. **Nunca deve ser alterada manualmente** — toda atualização vem via webhook.

```sql
create table konditor.faturas (
    id                  uuid         primary key default gen_random_uuid(),
    espaco_trabalho_id  uuid         not null references konditor.espacos_trabalho(id) on delete cascade,
    assinatura_id       uuid         references konditor.assinaturas(id) on delete set null,

    -- Dados do Stripe
    stripe_invoice_id   varchar(100) not null unique,         -- 'in_xxxxx'
    stripe_payment_intent_id varchar(100),                    -- 'pi_xxxxx'
    stripe_hosted_url   text,                                 -- URL da fatura no Stripe
    stripe_pdf_url      text,                                 -- PDF da fatura

    -- Valores
    valor_centavos      integer      not null,
    moeda               varchar(10)  not null default 'brl',
    descricao           text,

    -- Status
    status              varchar(30)  not null,
    -- 'draft' | 'open' | 'paid' | 'uncollectible' | 'void'

    -- Período faturado
    periodo_inicio      timestamptz,
    periodo_fim         timestamptz,

    -- Datas
    data_emissao        timestamptz  not null,
    data_vencimento     timestamptz,
    data_pagamento      timestamptz,

    -- Controle
    tentativas_cobranca integer      not null default 0,
    proximo_retry_em    timestamptz,

    criado_em           timestamptz  not null default now(),
    atualizado_em       timestamptz,

    constraint chk_faturas_status check (
        status in ('draft', 'open', 'paid', 'uncollectible', 'void')
    )
);

create index idx_faturas_espaco_trabalho on konditor.faturas (espaco_trabalho_id);
create index idx_faturas_assinatura      on konditor.faturas (assinatura_id);
create index idx_faturas_status          on konditor.faturas (status);
create index idx_faturas_data_emissao    on konditor.faturas (data_emissao desc);
```

---

### 3.5 Nova tabela `eventos_stripe`

Garante idempotência no processamento de webhooks. Cada evento do Stripe é registrado antes de ser processado.

```sql
create table konditor.eventos_stripe (
    id              uuid         primary key default gen_random_uuid(),
    stripe_event_id varchar(100) not null unique,   -- 'evt_xxxxx'
    tipo            varchar(100) not null,           -- ex: 'customer.subscription.updated'
    payload         jsonb        not null,           -- corpo completo do evento
    status          varchar(20)  not null default 'pendente',
    -- 'pendente' | 'processado' | 'ignorado' | 'erro'
    erro_mensagem   text,
    tentativas      integer      not null default 0,
    processado_em   timestamptz,
    criado_em       timestamptz  not null default now(),

    constraint chk_eventos_stripe_status check (
        status in ('pendente', 'processado', 'ignorado', 'erro')
    )
);

create index idx_eventos_stripe_tipo   on konditor.eventos_stripe (tipo);
create index idx_eventos_stripe_status on konditor.eventos_stripe (status);
```

---

## 4. Migration SQL (Flyway)

Criar o arquivo `V7__billing_stripe.sql` com todo o DDL das seções 3.1 a 3.5.

---

## 5. Entidades JPA

### `AssinaturaJpaEntity` (atualizar entidade existente)

```java
@Entity
@Table(name = "assinaturas", schema = "konditor")
public class AssinaturaJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espaco_trabalho_id", nullable = false)
    private EspacoTrabalhoJpaEntity espacoTrabalho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private DetalhesPlanoJpaEntity plano;

    @Column(name = "status", nullable = false)
    private String status; // active | trialing | past_due | inactive | cancelled

    @Column(name = "ciclo_cobranca", nullable = false)
    private String cicloCobranca; // monthly | yearly

    @Column(name = "id_assinatura_externo")
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id")
    private String stripeSubscriptionItemId;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "termina_em")
    private Instant terminaEm;

    @Column(name = "trial_termina_em")
    private Instant trialTerminaEm;

    @Column(name = "cancelamento_agendado")
    private boolean cancelamentoAgendado;

    @Column(name = "cancelamento_em")
    private Instant cancelamentoEm;

    @Column(name = "grace_period_ate")
    private Instant gracePeriodAte;

    @Column(name = "ultimo_pagamento_em")
    private Instant ultimoPagamentoEm;

    @Column(name = "proxima_cobranca_em")
    private Instant proximaCobrancaEm;

    @Column(name = "ultimo_evento_stripe_id")
    private String ultimoEventoStripeId;

    @Column(name = "ultimo_evento_stripe_em")
    private Instant ultimoEventoStripeEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por")
    private UserJpaEntity criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por")
    private UserJpaEntity atualizadoPor;

    @PrePersist void prePersist() { this.criadoEm = Instant.now(); }
    @PreUpdate  void preUpdate()  { this.atualizadoEm = Instant.now(); }
}
```

### `FaturaJpaEntity`

```java
@Entity
@Table(name = "faturas", schema = "konditor")
public class FaturaJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espaco_trabalho_id", nullable = false)
    private EspacoTrabalhoJpaEntity espacoTrabalho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id")
    private AssinaturaJpaEntity assinatura;

    @Column(name = "stripe_invoice_id", nullable = false, unique = true)
    private String stripeInvoiceId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_hosted_url")
    private String stripeHostedUrl;

    @Column(name = "stripe_pdf_url")
    private String stripePdfUrl;

    @Column(name = "valor_centavos", nullable = false)
    private int valorCentavos;

    @Column(name = "moeda", nullable = false)
    private String moeda;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "periodo_inicio")
    private Instant periodoInicio;

    @Column(name = "periodo_fim")
    private Instant periodoFim;

    @Column(name = "data_emissao", nullable = false)
    private Instant dataEmissao;

    @Column(name = "data_vencimento")
    private Instant dataVencimento;

    @Column(name = "data_pagamento")
    private Instant dataPagamento;

    @Column(name = "tentativas_cobranca")
    private int tentativasCobranca;

    @Column(name = "proximo_retry_em")
    private Instant proximoRetryEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist void prePersist() { this.criadoEm = Instant.now(); }
    @PreUpdate  void preUpdate()  { this.atualizadoEm = Instant.now(); }
}
```

### `EventoStripeJpaEntity`

```java
@Entity
@Table(name = "eventos_stripe", schema = "konditor")
public class EventoStripeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload; // JSON raw

    @Column(name = "status", nullable = false)
    private String status; // pendente | processado | ignorado | erro

    @Column(name = "erro_mensagem")
    private String erroMensagem;

    @Column(name = "tentativas")
    private int tentativas;

    @Column(name = "processado_em")
    private Instant processadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist void prePersist() { this.criadoEm = Instant.now(); }
}
```

---

## 6. Configuração do Stripe

### 6.1 Dependência Maven

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>27.3.0</version>
</dependency>
```

### 6.2 Configuração (application.yaml)

```yaml
stripe:
  api-key: ${STRIPE_SECRET_KEY}          # sk_live_xxx / sk_test_xxx
  webhook-secret: ${STRIPE_WEBHOOK_SECRET} # whsec_xxx — gerado pelo Stripe CLI ou Dashboard
  success-url: ${STRIPE_SUCCESS_URL:https://app.konditor.com.br/billing/sucesso}
  cancel-url: ${STRIPE_CANCEL_URL:https://app.konditor.com.br/billing/planos}
  portal-return-url: ${STRIPE_PORTAL_RETURN_URL:https://app.konditor.com.br/billing}
  trial-days: 14
```

### 6.3 Bean de Configuração

```java
// com.api.konditor.app.config.application.StripeConfig.java
@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = this.apiKey;
    }

    // getters para webhookSecret, successUrl, cancelUrl, portalReturnUrl, trialDays
}
```

---

## 7. Serviço Stripe (Provider)

Criar em `com.api.konditor.infra.googleprovider` → `com.api.konditor.infra.stripeprovider`.

### `StripeGateway.java` (interface)

```java
public interface StripeGateway {

    /** Cria um Stripe Customer para o workspace. */
    String criarCustomer(String email, String nomeWorkspace, UUID workspaceId);

    /** Cria uma Checkout Session para nova assinatura ou upgrade. */
    CheckoutSessionResult criarCheckoutSession(CriarCheckoutSessionCommand command);

    /** Cria uma sessão do Billing Portal para o customer gerenciar sua assinatura. */
    String criarPortalSession(String stripeCustomerId, String returnUrl);

    /** Cancela uma assinatura imediatamente no Stripe. */
    void cancelarAssinatura(String stripeSubscriptionId);

    /** Agenda cancelamento ao final do período atual (cancel_at_period_end). */
    void agendarCancelamento(String stripeSubscriptionId);

    /** Reverte um cancelamento agendado. */
    void reverterCancelamento(String stripeSubscriptionId);

    /** Busca uma assinatura pelo ID no Stripe. */
    StripeSubscriptionData buscarAssinatura(String stripeSubscriptionId);

    /** Constrói e valida o Event a partir do payload e da assinatura do webhook. */
    com.stripe.model.Event construirEvento(String payload, String sigHeader);
}
```

### `StripeGatewayImpl.java`

Implementação concreta usando o SDK `stripe-java`. Cada método deve:
- Tratar `StripeException` e relançar como exceção de domínio `StripeIntegrationException`
- Logar o request/response em nível `DEBUG`
- Usar Idempotency Keys em operações de escrita (`RequestOptions.builder().setIdempotencyKey(...)`)

---

## 8. Use Case

### Interface `AssinaturaUseCase.java`

```java
public interface AssinaturaUseCase {

    /** Retorna os planos disponíveis com preços formatados. */
    ListaPlanosResponse listarPlanos();

    /** Retorna a assinatura ativa do workspace do usuário autenticado. */
    AssinaturaResponse buscarAssinaturaAtual(UUID workspaceId);

    /** Inicia o checkout para nova assinatura ou upgrade/downgrade de plano. */
    CheckoutResponse iniciarCheckout(UUID workspaceId, UUID usuarioId, IniciarCheckoutCommand command);

    /** Abre o Billing Portal do Stripe para o owner gerenciar sua assinatura. */
    PortalResponse abrirPortalBilling(UUID workspaceId, UUID usuarioId);

    /** Retorna o histórico de faturas paginado. */
    Page<FaturaResponse> listarFaturas(UUID workspaceId, Pageable pageable);

    /** Processa um evento recebido via webhook do Stripe. */
    void processarEventoStripe(String payload, String stripeSignature);
}
```

### Implementação `AssinaturaUseCaseImpl.java`

#### Método `iniciarCheckout`

```
1. Buscar workspace pelo ID — lançar 404 se não encontrado
2. Verificar se usuário é owner do workspace — lançar 403 se não for
3. Buscar plano pelo ID informado — lançar 404 se não encontrado
4. Validar que o plano não é 'free' (checkout só para planos pagos)
5. Determinar o stripe_price_id com base no cicloCobranca (monthly | yearly)
6. Se workspace.stripeCustomerId == null:
   a. Criar Customer no Stripe com email do owner e nome do workspace
   b. Persistir stripeCustomerId no workspace
7. Montar CriarCheckoutSessionCommand:
   - stripeCustomerId
   - stripePriceId
   - workspaceId (nos metadata do Stripe para uso no webhook)
   - trialDays (se assinatura atual for null ou status == 'cancelled')
   - successUrl / cancelUrl
8. Chamar stripeGateway.criarCheckoutSession(command)
9. Retornar CheckoutResponse{ checkoutUrl }
```

#### Método `processarEventoStripe`

```
1. Construir e validar o Event via stripeGateway.construirEvento(payload, sigHeader)
   → lançar WebhookSignatureException se inválido
2. Verificar idempotência: buscar EventoStripeJpaEntity pelo stripeEventId
   → se já existe com status 'processado', retornar imediatamente (200 OK)
3. Persistir o evento com status 'pendente'
4. Despachar para o handler correto pelo event.type:
   - checkout.session.completed       → handleCheckoutCompleted
   - customer.subscription.created    → handleSubscriptionCreated
   - customer.subscription.updated    → handleSubscriptionUpdated
   - customer.subscription.deleted    → handleSubscriptionDeleted
   - invoice.paid                     → handleInvoicePaid
   - invoice.payment_failed           → handleInvoicePaymentFailed
   - invoice.created                  → handleInvoiceCreated
   - customer.subscription.trial_will_end → handleTrialWillEnd
   - outros tipos                     → marcar como 'ignorado'
5. Atualizar evento para status 'processado' com processadoEm = now()
6. Em caso de erro não tratado: marcar como 'erro', salvar erroMensagem, re-throw para Stripe retentar
```

#### `handleCheckoutCompleted`

```
1. Extrair workspaceId dos metadata da checkout session
2. Extrair stripeSubscriptionId da checkout session
3. Buscar assinatura existente do workspace (se houver)
4. Se não existir: criar nova AssinaturaJpaEntity
5. Atualizar campos:
   - stripeSubscriptionId
   - status = 'trialing' (se trial) ou 'active'
   - cicloCobranca (extrair do price.recurring.interval)
   - stripePriceId
   - stripeSubscriptionItemId
   - iniciadoEm / trialTerminaEm / proximaCobrancaEm / terminaEm
6. Atualizar plano_id do workspace para o novo plano
7. Registrar audit log: operação 'SUBSCRIPTION_CHECKOUT_COMPLETED'
8. (Futuro) Disparar e-mail de boas-vindas via evento de domínio
```

#### `handleSubscriptionUpdated`

```
1. Buscar assinatura pelo stripeSubscriptionId
2. Comparar campos relevantes (status, price, cancelação agendada, datas)
3. Atualizar apenas campos que mudaram (evitar write desnecessário)
4. Se status mudou para 'past_due': definir gracePeriodAte = now() + 3 dias
5. Se status mudou para 'active': limpar gracePeriodAte
6. Atualizar plano_id do workspace se o price mudou
7. Registrar audit log
```

#### `handleInvoicePaid`

```
1. Upsert de FaturaJpaEntity pelo stripeInvoiceId
2. Definir status = 'paid', dataPagamento = now()
3. Atualizar assinatura: ultimoPagamentoEm = now(), status = 'active', gracePeriodAte = null
```

#### `handleInvoicePaymentFailed`

```
1. Upsert de FaturaJpaEntity com status = 'open'
2. Incrementar tentativasCobranca, definir proximoRetryEm
3. Atualizar assinatura: status = 'past_due', gracePeriodAte = now() + 3 dias
4. (Futuro) Disparar e-mail de cobrança falhada
```

---

## 9. API — Endpoints

### 9.1 Endpoints Públicos (sem autenticação)

---

#### `GET /planos`

Retorna os planos disponíveis para exibição na página de preços.

**Auth:** não requerida  
**Cache:** `Cache-Control: public, max-age=3600`

**Response `200 OK`:**
```json
{
  "planos": [
    {
      "id": "uuid-free",
      "nome": "free",
      "nomeExibido": "Gratuito",
      "descricao": "Para começar sem compromisso",
      "destaque": false,
      "badgeTexto": null,
      "preco": {
        "mensal": { "centavos": 0, "formatado": "Grátis" },
        "anual": { "centavos": 0, "formatado": "Grátis", "economiaPercentual": 0 }
      },
      "trialDias": 0,
      "limites": {
        "maxEspacosTrabalho": 1,
        "maxMembros": 1,
        "maxProdutos": 10,
        "maxIngredientes": 20,
        "maxPedidosPorMes": 20
      },
      "features": {
        "temCalculoCustos": true,
        "temGerenciamentoPedidos": false,
        "temRelatorios": false,
        "temLogAuditoria": false,
        "temAcessoApi": false,
        "temSuportePrioritario": false,
        "temExportacao": false
      }
    },
    {
      "id": "uuid-basic",
      "nome": "basic",
      "nomeExibido": "Básico",
      "descricao": "Para doceiros em crescimento",
      "destaque": false,
      "badgeTexto": null,
      "preco": {
        "mensal": { "centavos": 2990, "formatado": "R$ 29,90/mês" },
        "anual": { "centavos": 28704, "formatado": "R$ 287,04/ano", "economiaPercentual": 20 }
      },
      "trialDias": 14,
      "limites": { "...": "..." },
      "features": { "...": "..." }
    },
    {
      "id": "uuid-premium",
      "nome": "premium",
      "nomeExibido": "Premium",
      "descricao": "Para profissionais sem limites",
      "destaque": true,
      "badgeTexto": "Mais popular",
      "preco": {
        "mensal": { "centavos": 6990, "formatado": "R$ 69,90/mês" },
        "anual": { "centavos": 67104, "formatado": "R$ 671,04/ano", "economiaPercentual": 20 }
      },
      "trialDias": 14,
      "limites": { "...": "..." },
      "features": { "...": "..." }
    }
  ]
}
```

---

### 9.2 Endpoints Autenticados — Owner do Workspace

> Todos os endpoints abaixo exigem **Bearer JWT válido**. Operações de escrita (checkout, portal, cancelamento) exigem que o usuário autenticado seja o **`owner`** do workspace referenciado. Membros com papel `admin` ou `member` recebem `403`.

---

#### `GET /billing/assinatura`

Retorna o estado atual da assinatura do workspace do usuário autenticado.

**Auth:** Bearer JWT  
**Query param:** `workspaceId` (UUID) — obrigatório

**Response `200 OK`:**
```json
{
  "assinaturaId": "uuid",
  "plano": {
    "id": "uuid-premium",
    "nome": "premium",
    "nomeExibido": "Premium"
  },
  "status": "active",
  "cicloCobranca": "monthly",
  "iniciadoEm": "2025-11-01T00:00:00Z",
  "proximaCobrancaEm": "2026-12-01T00:00:00Z",
  "trialTerminaEm": null,
  "cancelamentoAgendado": false,
  "cancelamentoEm": null,
  "limites": {
    "maxEspacosTrabalho": 3,
    "maxMembros": 10,
    "maxProdutos": null,
    "maxIngredientes": null,
    "maxPedidosPorMes": null
  }
}
```

**Response `200 OK`** (plano free / sem assinatura ativa):
```json
{
  "assinaturaId": null,
  "plano": {
    "id": "uuid-free",
    "nome": "free",
    "nomeExibido": "Gratuito"
  },
  "status": "active",
  "cicloCobranca": null,
  "iniciadoEm": null,
  "proximaCobrancaEm": null,
  "trialTerminaEm": null,
  "cancelamentoAgendado": false,
  "cancelamentoEm": null,
  "limites": { "...": "..." }
}
```

---

#### `POST /billing/checkout`

Inicia uma sessão de checkout no Stripe para assinar ou fazer upgrade de plano.  
Retorna a URL da página de checkout hospedada pelo Stripe.

**Auth:** Bearer JWT + Role `owner`

**Request body:**
```json
{
  "workspaceId": "uuid-workspace",
  "planoId": "uuid-premium",
  "cicloCobranca": "monthly"
}
```

**Validações:**
- `workspaceId` — obrigatório, workspace deve existir e usuário deve ser `owner`
- `planoId` — obrigatório, deve existir e não pode ser o plano `free`
- `cicloCobranca` — obrigatório, `monthly` ou `yearly`
- Não é possível abrir checkout para o mesmo plano e ciclo já contratados (retorna `409`)

**Response `200 OK`:**
```json
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_live_xxxxx"
}
```

**Response `409 Conflict`:**
```json
{
  "status": 409,
  "erro": "O workspace já possui o plano 'Premium' no ciclo 'mensal' ativo."
}
```

**Response `403 Forbidden`:**
```json
{
  "status": 403,
  "erro": "Apenas o proprietário do workspace pode gerenciar a assinatura."
}
```

---

#### `POST /billing/portal`

Cria uma sessão do **Stripe Billing Portal** — o owner é redirecionado para a interface do Stripe para:
- Atualizar método de pagamento (cartão)
- Ver histórico de faturas no Stripe
- Fazer upgrade/downgrade de plano
- Cancelar assinatura

**Auth:** Bearer JWT + Role `owner`

**Request body:**
```json
{
  "workspaceId": "uuid-workspace"
}
```

**Validações:**
- Workspace deve ter `stripeCustomerId` configurado (ter feito ao menos um checkout)

**Response `200 OK`:**
```json
{
  "portalUrl": "https://billing.stripe.com/session/test_xxxxx"
}
```

**Response `400 Bad Request`** (sem Customer Stripe):
```json
{
  "status": 400,
  "erro": "Nenhuma assinatura encontrada para este workspace. Faça um checkout primeiro."
}
```

---

#### `GET /billing/faturas`

Lista o histórico de faturas do workspace com paginação.

**Auth:** Bearer JWT + Role `owner`  
**Query params:** `workspaceId` (obrigatório), `page` (default: 0), `size` (default: 10, máx: 50)

**Response `200 OK`:**
```json
{
  "conteudo": [
    {
      "id": "uuid-fatura",
      "stripeInvoiceId": "in_xxxxx",
      "status": "paid",
      "valorCentavos": 6990,
      "valorFormatado": "R$ 69,90",
      "moeda": "brl",
      "descricao": "Assinatura Konditor Premium × 1",
      "periodoInicio": "2025-11-01T00:00:00Z",
      "periodoFim": "2025-12-01T00:00:00Z",
      "dataEmissao": "2025-11-01T00:00:00Z",
      "dataPagamento": "2025-11-01T00:02:13Z",
      "stripeHostedUrl": "https://invoice.stripe.com/i/acct_xxx/live_YYY",
      "stripePdfUrl": "https://pay.stripe.com/invoice/acct_xxx/live_YYY/pdf"
    }
  ],
  "pagina": 0,
  "tamanhoPagina": 10,
  "totalItens": 7,
  "totalPaginas": 1
}
```

---

### 9.3 Webhook Stripe

---

#### `POST /stripe/webhook`

Endpoint exclusivo para receber eventos do Stripe. **Não usa autenticação JWT** — a autenticidade é validada via `Stripe-Signature` header usando o `webhook-secret`.

**Segurança:**
- Este endpoint deve estar **fora** do filtro de autenticação Spring Security
- A validação da assinatura (`Webhook.constructEvent`) deve ocorrer **antes** de qualquer lógica de negócio
- Em caso de falha na validação da assinatura: retornar `400 Bad Request` sem body
- O endpoint deve responder `200 OK` rapidamente — processamento assíncrono pesado deve usar `@Async` ou uma fila

**Headers obrigatórios:**
- `Stripe-Signature: t=xxxxxxxx,v1=xxxxxxxx`
- `Content-Type: application/json`

**Response:** `200 OK` (sem body) — qualquer status 4xx ou 5xx fará o Stripe retentar

**Eventos tratados:**

| Evento Stripe                               | Ação no Sistema                                                          |
|---------------------------------------------|--------------------------------------------------------------------------|
| `checkout.session.completed`                | Ativa assinatura, atualiza plano do workspace, registra audit log        |
| `customer.subscription.created`             | Cria/atualiza registro local de assinatura                               |
| `customer.subscription.updated`             | Sincroniza status, datas, plano. Define grace period se `past_due`      |
| `customer.subscription.deleted`             | Marca assinatura como `cancelled`, rebaixa workspace para plano `free`   |
| `invoice.paid`                              | Upsert fatura com status `paid`, atualiza `ultimoPagamentoEm`            |
| `invoice.payment_failed`                    | Upsert fatura, marca assinatura `past_due`, define grace period          |
| `invoice.created`                           | Upsert fatura com status `open`                                          |
| `customer.subscription.trial_will_end`      | (Futuro) Dispara e-mail de aviso 3 dias antes do trial expirar          |

> **Idempotência:** Antes de processar qualquer evento, verificar `eventos_stripe.stripe_event_id`. Se já existe com status `processado`, retornar `200 OK` imediatamente sem reprocessar.

---

### 9.4 Endpoints Administrativos

> Todos exigem Role `owner` **global** (usuário interno do sistema, não owner de workspace). Implementar com um role especial `system_admin` ou via header de API key interna.

---

#### `GET /admin/billing/assinaturas`

Lista todas as assinaturas com filtros para gestão operacional.

**Auth:** Bearer JWT + Role `system_admin` (ou API Key)

**Query params:**
| Param    | Tipo    | Padrão | Descrição                                                    |
|----------|---------|--------|--------------------------------------------------------------|
| status   | string  | —      | Filtra por status (`active`, `trialing`, `past_due`, ...)    |
| plano    | string  | —      | Filtra por nome do plano (`free`, `basic`, `premium`)        |
| ciclo    | string  | —      | Filtra por ciclo (`monthly`, `yearly`)                       |
| page     | integer | 0      | Página (0-based)                                             |
| size     | integer | 20     | Itens por página (máx. 100)                                  |

**Response `200 OK`:**
```json
{
  "conteudo": [
    {
      "assinaturaId": "uuid",
      "workspace": { "id": "uuid", "nome": "Doces da Ana" },
      "owner": { "id": "uuid", "email": "ana@exemplo.com", "nome": "Ana Lima" },
      "plano": "premium",
      "status": "active",
      "cicloCobranca": "yearly",
      "proximaCobrancaEm": "2026-11-01T00:00:00Z",
      "cancelamentoAgendado": false
    }
  ],
  "pagina": 0,
  "tamanhoPagina": 20,
  "totalItens": 342,
  "totalPaginas": 18
}
```

---

#### `GET /admin/billing/metricas`

Retorna métricas de receita para o dashboard financeiro interno.

**Auth:** Bearer JWT + Role `system_admin`

**Response `200 OK`:**
```json
{
  "mrr": {
    "valorCentavos": 1842510,
    "valorFormatado": "R$ 18.425,10",
    "variacaoMesAnteriorPercentual": 4.3
  },
  "arr": {
    "valorCentavos": 22110120,
    "valorFormatado": "R$ 221.101,20"
  },
  "totalAssinaturas": 342,
  "porStatus": {
    "active": 298,
    "trialing": 27,
    "past_due": 11,
    "cancelled": 6
  },
  "porPlano": {
    "free": 1045,
    "basic": 187,
    "premium": 155
  },
  "porCiclo": {
    "monthly": 241,
    "yearly": 101
  },
  "churnMesAtual": {
    "quantidade": 6,
    "percentual": 1.75
  },
  "novosMesAtual": 47,
  "upgrades": 12,
  "downgrades": 3
}
```

---

## 10. Regras de Negócio

| # | Regra |
|---|-------|
| 1 | Apenas o `owner` do workspace pode iniciar checkout, abrir o portal de billing, ou cancelar a assinatura. Membros `admin` e `member` recebem `403`. |
| 2 | Um workspace nunca deve ter mais de uma assinatura com status `active` ou `trialing` simultaneamente. A transição de plano deve ser feita via upgrade/downgrade no Stripe (substituição do subscription item), não criando uma nova assinatura. |
| 3 | O plano `free` não possui checkout no Stripe. O rebaixamento para `free` ocorre automaticamente via webhook `customer.subscription.deleted`. |
| 4 | O trial de 14 dias é concedido apenas na **primeira** assinatura paga do workspace. Reativações após cancelamento não ganham novo trial. |
| 5 | Após falha de pagamento (`invoice.payment_failed`), conceder **grace period de 3 dias** antes de alterar o status para `inactive`. Durante o grace period, o acesso é mantido normalmente. |
| 6 | O `stripeCustomerId` do workspace deve ser reutilizado em todos os checkouts. Nunca criar dois Customers para o mesmo workspace. |
| 7 | O processamento de webhooks deve ser **idempotente**: verificar `eventos_stripe.stripe_event_id` antes de processar. Eventos duplicados retornam `200 OK` imediatamente. |
| 8 | O endpoint `POST /stripe/webhook` deve ser excluído do filtro JWT. A autenticidade é garantida exclusivamente via `Stripe-Signature`. |
| 9 | Todo downgrade de plano deve verificar se o workspace viola os novos limites (ex: 5 membros em plano que permite 3). Nesse caso, não bloquear o downgrade, mas emitir um aviso no próximo login. |
| 10 | Audit log deve ser registrado em `logs_auditoria` para: checkout iniciado, assinatura ativada, upgrade/downgrade, cancelamento agendado, cancelamento efetivado, reativação. |
| 11 | Os preços monetários devem ser armazenados em **centavos** (integer) e formatados apenas na camada de apresentação (response DTO). Nunca usar `float` ou `double` para valores financeiros. |
| 12 | O cancelamento via `POST /billing/portal` deve sempre usar o **Stripe Billing Portal** (não cancelar diretamente pela API). Isso garante conformidade com as políticas cardboard do Stripe e não viola a política de cancellamento fácil exigida por Apple/Google se houver app mobile. |

---

## 11. Fluxo Completo — Novo Usuário Assinando

```
1. Usuário acessa /planos
   └── GET /planos  →  exibe cards com preços e features

2. Usuário clica "Começar agora" no plano Premium
   └── POST /billing/checkout { workspaceId, planoId, cicloCobranca: "monthly" }
         ├── Backend cria Stripe Customer (se necessário)
         └── Backend cria Checkout Session
               └── Retorna { checkoutUrl }

3. Frontend redireciona para checkout.stripe.com
   ├── Usuário preenche dados do cartão
   └── Stripe processa o pagamento

4. Stripe redireciona para successUrl (/billing/sucesso)
   + Stripe dispara webhook → POST /stripe/webhook

5. Backend processa checkout.session.completed
   ├── Cria/atualiza assinatura com status 'trialing' (14 dias)
   ├── Atualiza plano do workspace para 'premium'
   └── Registra audit log

6. Frontend exibe tela de sucesso
   └── GET /billing/assinatura  →  confirma status 'trialing'
```

---

## 12. Fluxo Completo — Gerenciar Assinatura (Portal)

```
1. Owner acessa /billing
   └── GET /billing/assinatura  →  exibe plano atual, próxima cobrança, status

2. Owner clica "Gerenciar assinatura"
   └── POST /billing/portal { workspaceId }
         └── Backend cria Billing Portal Session
               └── Retorna { portalUrl }

3. Frontend redireciona para billing.stripe.com
   ├── Usuário pode: trocar cartão | ver faturas | cancelar | fazer upgrade
   └── Após ação, Stripe redireciona de volta para portalReturnUrl (/billing)

4. Stripe dispara webhook com a mudança realizada
   └── Backend processa customer.subscription.updated | deleted

5. Frontend recarrega GET /billing/assinatura com o novo estado
```

---

## 13. Estrutura de Pacotes

```
com.api.konditor
├── app
│   └── controller
│       ├── BillingController.java           # GET /planos, /billing/*, POST /billing/*
│       └── StripeWebhookController.java     # POST /stripe/webhook (sem auth)
│   └── controller
│       ├── request
│       │   ├── IniciarCheckoutRequest.java
│       │   └── AbrirPortalRequest.java
│       └── response
│           ├── ListaPlanosResponse.java
│           ├── PlanoResponse.java
│           ├── AssinaturaResponse.java
│           ├── CheckoutResponse.java
│           ├── PortalResponse.java
│           └── FaturaResponse.java
│
├── domain
│   ├── enuns
│   │   └── CicloCobranca.java               # monthly | yearly
│   └── usecase
│       ├── AssinaturaUseCase.java            # interface
│       └── impl
│           └── AssinaturaUseCaseImpl.java
│
└── infra
    ├── stripeprovider
    │   ├── StripeGateway.java                # interface
    │   ├── StripeGatewayImpl.java            # implementação com stripe-java SDK
    │   ├── StripeConfig.java                 # @ConfigurationProperties + init()
    │   └── dto
    │       ├── CheckoutSessionResult.java
    │       ├── CriarCheckoutSessionCommand.java
    │       └── StripeSubscriptionData.java
    └── jpa
        ├── entity
        │   ├── AssinaturaJpaEntity.java       # atualizar
        │   ├── FaturaJpaEntity.java           # novo
        │   └── EventoStripeJpaEntity.java     # novo
        └── repository
            ├── AssinaturaJpaRepository.java   # atualizar
            ├── FaturaJpaRepository.java       # novo
            └── EventoStripeJpaRepository.java # novo
```

---

## 14. Repositórios JPA

### `AssinaturaJpaRepository` (atualizar)

```java
public interface AssinaturaJpaRepository extends JpaRepository<AssinaturaJpaEntity, UUID> {

    Optional<AssinaturaJpaEntity> findByEspacoTrabalhoIdAndExcluidoEmIsNull(UUID workspaceId);

    Optional<AssinaturaJpaEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<AssinaturaJpaEntity> findAllByStatusAndExcluidoEmIsNull(String status);

    // Para relatório MRR — agrupamento por plano e ciclo
    @Query("""
        SELECT a.plano.nome, a.cicloCobranca, COUNT(a), SUM(dp.precoCentavos)
        FROM AssinaturaJpaEntity a
        JOIN a.plano dp
        WHERE a.status IN ('active', 'trialing')
          AND a.excluidoEm IS NULL
        GROUP BY a.plano.nome, a.cicloCobranca
        """)
    List<Object[]> findAggregatedMRR();
}
```

### `FaturaJpaRepository`

```java
public interface FaturaJpaRepository extends JpaRepository<FaturaJpaEntity, UUID> {

    Page<FaturaJpaEntity> findAllByEspacoTrabalhoIdOrderByDataEmissaoDesc(
        UUID workspaceId, Pageable pageable);

    Optional<FaturaJpaEntity> findByStripeInvoiceId(String stripeInvoiceId);
}
```

### `EventoStripeJpaRepository`

```java
public interface EventoStripeJpaRepository extends JpaRepository<EventoStripeJpaEntity, UUID> {

    Optional<EventoStripeJpaEntity> findByStripeEventId(String stripeEventId);

    List<EventoStripeJpaEntity> findAllByStatusOrderByCriadoEmAsc(String status);
}
```

---

## 15. Segurança

### Exclusão do Filtro JWT para o Webhook

```java
// SecurityConfig.java — adicionar no requestMatcher de rotas públicas
.requestMatchers("/stripe/webhook").permitAll()
```

> **Atenção:** O endpoint `/stripe/webhook` **não deve ter CSRF protection** ativo. Garantir que `csrf().ignoringRequestMatchers("/stripe/webhook")` está configurado, ou que o CSRF está desabilitado globalmente para APIs (stateless JWT).

### Validação da Assinatura do Webhook

```java
// StripeWebhookController.java
@PostMapping("/stripe/webhook")
public ResponseEntity<Void> receberEvento(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String sigHeader) {

    try {
        assinaturaUseCase.processarEventoStripe(payload, sigHeader);
        return ResponseEntity.ok().build();
    } catch (WebhookSignatureException e) {
        log.warn("Assinatura Stripe inválida: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        log.error("Erro ao processar webhook Stripe", e);
        return ResponseEntity.internalServerError().build(); // Stripe vai retentar
    }
}
```

---

## 16. Testes

### Testes Unitários (`AssinaturaUseCaseImplTest`)

| Cenário | Resultado esperado |
|---------|--------------------|
| `iniciarCheckout` com plano free | Lança `IllegalArgumentException` |
| `iniciarCheckout` com plano já ativo | Lança `ConflictException` (409) |
| `iniciarCheckout` sem stripeCustomerId | Cria Customer no Stripe e persiste |
| `iniciarCheckout` usuário não-owner | Lança `ForbiddenException` (403) |
| `processarEventoStripe` com assinatura inválida | Lança `WebhookSignatureException` |
| `processarEventoStripe` evento duplicado | Retorna sem reprocessar (idempotência) |
| `handleCheckoutCompleted` → nova assinatura | Cria assinatura com status correto |
| `handleInvoicePaymentFailed` | Define grace period em 3 dias |
| `handleSubscriptionDeleted` | Status = cancelled, plano = free |
| `listarFaturas` com paginação | Retorna página correta |

### Testes de Integração (Stripe CLI / TestContainers)

Para homologação local:

```bash
# Instalar Stripe CLI
# https://stripe.com/docs/stripe-cli

# Escutar eventos e repassar para o backend local
stripe listen --forward-to localhost:8080/stripe/webhook

# Simular eventos específicos
stripe trigger checkout.session.completed
stripe trigger customer.subscription.updated
stripe trigger invoice.payment_failed
stripe trigger customer.subscription.deleted
```

---

## 17. Considerações de Deploy

### Variáveis de Ambiente Necessárias

| Variável                   | Ambiente     | Descrição                                     |
|----------------------------|--------------|-----------------------------------------------|
| `STRIPE_SECRET_KEY`        | prod/staging | `sk_live_xxx` / `sk_test_xxx`                 |
| `STRIPE_WEBHOOK_SECRET`    | prod/staging | `whsec_xxx` — gerado no Dashboard ou CLI      |
| `STRIPE_SUCCESS_URL`       | prod/staging | URL de retorno após checkout com sucesso      |
| `STRIPE_CANCEL_URL`        | prod/staging | URL de retorno após abandono do checkout      |
| `STRIPE_PORTAL_RETURN_URL` | prod/staging | URL de retorno após fechar o Billing Portal   |

### Setup no Stripe Dashboard (uma vez por ambiente)

1. Criar Products para cada plano pago (`basic`, `premium`)
2. Criar Prices mensais e anuais para cada Product
3. Anotar os `price_id`s e preencher no seed da tabela `detalhes_plano`
4. Configurar o Webhook Endpoint apontando para `https://api.konditor.com.br/stripe/webhook`
5. Selecionar os eventos: todos os listados na seção 9.3
6. Copiar o `Signing Secret` para a variável `STRIPE_WEBHOOK_SECRET`
7. Configurar o Billing Portal via Dashboard → Billing → Customer Portal

---

## 18. Critérios de Aceitação

- [ ] `GET /planos` retorna os 3 planos com preços mensais e anuais sem autenticação
- [ ] `POST /billing/checkout` retorna `checkoutUrl` válida e cria Customer no Stripe se necessário
- [ ] `POST /billing/checkout` retorna `409` ao tentar assinar o plano já contratado
- [ ] `POST /billing/checkout` retorna `403` para usuário não-owner
- [ ] `POST /billing/portal` retorna `portalUrl` válida para workspace com Customer existente
- [ ] `POST /billing/portal` retorna `400` para workspace sem histórico de checkout
- [ ] `POST /stripe/webhook` processa `checkout.session.completed` e ativa a assinatura
- [ ] `POST /stripe/webhook` processa `invoice.payment_failed` e define grace period
- [ ] `POST /stripe/webhook` processa `customer.subscription.deleted` e rebaixa para `free`
- [ ] Webhooks duplicados são ignorados (idempotência via `eventos_stripe`)
- [ ] `POST /stripe/webhook` retorna `400` para payload com assinatura inválida
- [ ] `GET /billing/faturas` retorna faturas paginadas apenas para o owner do workspace
- [ ] `GET /billing/assinatura` retorna status correto para workspace no trial
- [ ] `GET /billing/assinatura` retorna dados do plano free para workspace sem assinatura paga
- [ ] `GET /admin/billing/metricas` retorna MRR calculado corretamente
- [ ] Audit log registrado para checkout, ativação, upgrade, cancelamento
- [ ] Testes unitários com cobertura dos casos de negócio listados na seção 16
- [ ] Nenhum valor monetário usa `float` ou `double` — apenas `integer` (centavos) ou `BigDecimal`
````

