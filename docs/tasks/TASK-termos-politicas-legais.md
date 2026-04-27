# TASK — Controle de Termos Legais e Consentimento do Usuário

**Módulo:** Legal / Compliance  
**Prioridade:** Alta  
**Estimativa:** 5–8 dias  
**Contexto:** Konditor SaaS — Spring Boot 4.0.5, DDD, PostgreSQL, multi-tenant por workspace

---

## 1. Objetivo

Implementar um sistema completo de gerenciamento de **Termos de Uso**, **Política de Privacidade** e **Política de Cookies**, com:

- Versionamento semântico de cada documento legal
- Ativação controlada de versões (apenas uma ativa por tipo de documento por vez)
- Rastreamento de consentimento do usuário (qual versão aceitou, quando, de qual IP e user-agent)
- Endpoints públicos para o frontend consumir os documentos vigentes
- Endpoints administrativos para criar e publicar novas versões
- Bloqueio de acesso ao app quando o usuário ainda não aceitou a versão atual

---

## 2. Tipos de Documento

```
TERMS_OF_USE          → Termos de Uso
PRIVACY_POLICY        → Política de Privacidade
COOKIE_POLICY         → Política de Cookies
```

Todos os tipos seguem o mesmo ciclo de vida: `rascunho → ativo → substituído`.

---

## 3. Modelo de Banco de Dados

### 3.1 Tabela `documentos_legais`

Armazena todas as versões de todos os tipos de documentos legais.

```sql
create table konditor.documentos_legais (
    id              uuid primary key default gen_random_uuid(),

    tipo            varchar(30)  not null,                       -- 'TERMS_OF_USE' | 'PRIVACY_POLICY' | 'COOKIE_POLICY'
    versao          varchar(20)  not null,                       -- ex: '1.0.0', '1.1.0', '2.0.0'
    titulo          varchar(255) not null,                       -- ex: 'Termos de Uso — versão 2.0'
    conteudo        text         not null,                       -- HTML ou Markdown do documento completo
    resumo          text,                                        -- resumo das mudanças em relação à versão anterior
    status          varchar(20)  not null default 'rascunho',    -- 'rascunho' | 'ativo' | 'substituido'
    obrigatorio     boolean      not null default true,          -- se false, o usuário pode recusar e continuar usando

    ativo_desde     timestamptz,                                 -- quando foi publicado/ativado
    substituido_em  timestamptz,                                 -- quando foi substituído por outra versão

    criado_em       timestamptz  not null default now(),
    atualizado_em   timestamptz,
    excluido_em     timestamptz,

    criado_por      uuid references konditor.usuarios(id),
    atualizado_por  uuid references konditor.usuarios(id),

    constraint uq_documentos_legais_tipo_versao unique (tipo, versao),
    constraint chk_documentos_legais_status
        check (status in ('rascunho', 'ativo', 'substituido')),
    constraint chk_documentos_legais_tipo
        check (tipo in ('TERMS_OF_USE', 'PRIVACY_POLICY', 'COOKIE_POLICY'))
);

-- Garante que só exista 1 documento ativo por tipo por vez
create unique index uq_documentos_legais_ativo_por_tipo
    on konditor.documentos_legais (tipo)
    where status = 'ativo';

create index idx_documentos_legais_tipo_status
    on konditor.documentos_legais (tipo, status);

create index idx_documentos_legais_criado_em
    on konditor.documentos_legais (criado_em desc);
```

---

### 3.2 Tabela `consentimentos_usuario`

Registra **imutavelmente** cada aceite ou recusa do usuário. Nunca deve ser alterada ou deletada.

```sql
create table konditor.consentimentos_usuario (
    id                  uuid primary key default gen_random_uuid(),

    usuario_id          uuid        not null references konditor.usuarios(id),
    documento_legal_id  uuid        not null references konditor.documentos_legais(id),

    aceito              boolean     not null,                    -- true = aceito, false = recusado
    ip_origem           varchar(45),                             -- IPv4 ou IPv6
    user_agent          text,                                    -- navegador/dispositivo
    metodo_aceite       varchar(30) not null default 'MANUAL',  -- 'MANUAL' | 'API' | 'IMPORTACAO'

    criado_em           timestamptz not null default now(),

    -- Um registro por usuário/documento (upsert via ON CONFLICT)
    constraint uq_consentimentos_usuario_doc unique (usuario_id, documento_legal_id)
);

create index idx_consentimentos_usuario_id
    on konditor.consentimentos_usuario (usuario_id);

create index idx_consentimentos_doc_id
    on konditor.consentimentos_usuario (documento_legal_id);

create index idx_consentimentos_criado_em
    on konditor.consentimentos_usuario (criado_em desc);
```

> **Regra:** Esta tabela nunca recebe UPDATE nem DELETE. Para revogar/re-aceitar, insere-se novo registro (lógica de upsert pelo `unique constraint`).

---

## 4. Entidades JPA

### `DocumentoLegalJpaEntity`

```java
@Entity
@Table(name = "documentos_legais", schema = "konditor")
public class DocumentoLegalJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumentoLegal tipo;

    @Column(name = "versao", nullable = false, length = 20)
    private String versao;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "conteudo", nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "resumo", columnDefinition = "text")
    private String resumo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusDocumentoLegal status;

    @Column(name = "obrigatorio", nullable = false)
    private boolean obrigatorio;

    @Column(name = "ativo_desde")
    private Instant ativoDesdé;

    @Column(name = "substituido_em")
    private Instant substituidoEm;

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

### `ConsentimentoUsuarioJpaEntity`

```java
@Entity
@Table(name = "consentimentos_usuario", schema = "konditor")
public class ConsentimentoUsuarioJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserJpaEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_legal_id", nullable = false)
    private DocumentoLegalJpaEntity documentoLegal;

    @Column(name = "aceito", nullable = false)
    private boolean aceito;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_aceite", nullable = false, length = 30)
    private MetodoAceite metodoAceite;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist void prePersist() { this.criadoEm = Instant.now(); }
}
```

### Enums

```java
public enum TipoDocumentoLegal {
    TERMS_OF_USE,
    PRIVACY_POLICY,
    COOKIE_POLICY
}

public enum StatusDocumentoLegal {
    rascunho,
    ativo,
    substituido
}

public enum MetodoAceite {
    MANUAL,   // usuário clicou em "Aceitar" na UI
    API,      // aceite via integração
    IMPORTACAO
}
```

---

## 5. Repositórios JPA

### `DocumentoLegalJpaRepository`

```java
public interface DocumentoLegalJpaRepository
    extends JpaRepository<DocumentoLegalJpaEntity, UUID> {

    // Busca o documento ativo para um tipo
    Optional<DocumentoLegalJpaEntity> findByTipoAndStatusAndExcluidoEmIsNull(
        TipoDocumentoLegal tipo, StatusDocumentoLegal status);

    // Busca todos os documentos ativos (para o endpoint público que retorna todos de uma vez)
    List<DocumentoLegalJpaEntity> findAllByStatusAndExcluidoEmIsNull(
        StatusDocumentoLegal status);

    // Histórico de versões de um tipo
    List<DocumentoLegalJpaEntity> findAllByTipoAndExcluidoEmIsNullOrderByCriadoEmDesc(
        TipoDocumentoLegal tipo);

    // Verifica duplicidade de versão por tipo
    boolean existsByTipoAndVersaoAndExcluidoEmIsNull(
        TipoDocumentoLegal tipo, String versao);

    // Busca o ativo por tipo para ativação (lock para evitar race condition)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DocumentoLegalJpaEntity d WHERE d.tipo = :tipo AND d.status = 'ativo' AND d.excluidoEm IS NULL")
    Optional<DocumentoLegalJpaEntity> findAtivoParaSubstituicao(@Param("tipo") TipoDocumentoLegal tipo);
}
```

### `ConsentimentoUsuarioJpaRepository`

```java
public interface ConsentimentoUsuarioJpaRepository
    extends JpaRepository<ConsentimentoUsuarioJpaEntity, UUID> {

    // Verifica se o usuário aceitou uma versão específica
    Optional<ConsentimentoUsuarioJpaEntity> findByUsuarioIdAndDocumentoLegalId(
        UUID usuarioId, UUID documentoLegalId);

    // Todos os consentimentos de um usuário
    List<ConsentimentoUsuarioJpaEntity> findAllByUsuarioIdOrderByCriadoEmDesc(
        UUID usuarioId);

    // Verifica se o usuário aceitou o documento ativo de um tipo
    @Query("""
        SELECT c FROM ConsentimentoUsuarioJpaEntity c
        JOIN c.documentoLegal d
        WHERE c.usuario.id = :usuarioId
          AND d.tipo = :tipo
          AND d.status = 'ativo'
          AND c.aceito = true
        """)
    Optional<ConsentimentoUsuarioJpaEntity> findAceiteAtivoByUsuarioAndTipo(
        @Param("usuarioId") UUID usuarioId,
        @Param("tipo") TipoDocumentoLegal tipo);

    // Lista quais documentos ativos o usuário ainda NÃO aceitou
    @Query("""
        SELECT d FROM DocumentoLegalJpaEntity d
        WHERE d.status = 'ativo'
          AND d.obrigatorio = true
          AND d.excluidoEm IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM ConsentimentoUsuarioJpaEntity c
              WHERE c.usuario.id = :usuarioId
                AND c.documentoLegal.id = d.id
                AND c.aceito = true
          )
        """)
    List<DocumentoLegalJpaEntity> findDocumentosPendentesDeAceite(
        @Param("usuarioId") UUID usuarioId);
}
```

---

## 6. API — Endpoints

### 6.1 Endpoints Públicos (sem autenticação)

---

#### `GET /legal/documentos/vigentes`

Retorna todos os documentos legais ativos em uma única chamada.  
**Uso:** o frontend chama este endpoint ao inicializar o app para exibir os documentos vigentes.

**Headers:** nenhum obrigatório  
**Auth:** não requerida

**Response `200 OK`:**
```json
{
  "documentos": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "tipo": "TERMS_OF_USE",
      "versao": "2.1.0",
      "titulo": "Termos de Uso — versão 2.1",
      "conteudo": "<p>...</p>",
      "resumo": "Adicionamos a seção de assinaturas e pagamentos recorrentes.",
      "obrigatorio": true,
      "ativoDesdé": "2025-11-01T00:00:00Z"
    },
    {
      "id": "4ab96g75-6828-5673-c4gd-3d074g77bgb7",
      "tipo": "PRIVACY_POLICY",
      "versao": "1.3.0",
      "titulo": "Política de Privacidade — versão 1.3",
      "conteudo": "<p>...</p>",
      "resumo": "Inclusão do DPA para clientes empresariais (LGPD Art. 37).",
      "obrigatorio": true,
      "ativoDesdé": "2025-09-15T00:00:00Z"
    },
    {
      "id": "5bc07h86-7939-6784-d5he-4e185h88chc8",
      "tipo": "COOKIE_POLICY",
      "versao": "1.0.0",
      "titulo": "Política de Cookies — versão 1.0",
      "conteudo": "<p>...</p>",
      "resumo": null,
      "obrigatorio": false,
      "ativoDesdé": "2025-06-01T00:00:00Z"
    }
  ]
}
```

---

#### `GET /legal/documentos/{tipo}`

Retorna apenas o documento ativo de um tipo específico.

**Path params:**
| Param | Tipo   | Descrição                                              |
|-------|--------|--------------------------------------------------------|
| tipo  | string | `TERMS_OF_USE`, `PRIVACY_POLICY` ou `COOKIE_POLICY`   |

**Response `200 OK`:**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "tipo": "TERMS_OF_USE",
  "versao": "2.1.0",
  "titulo": "Termos de Uso — versão 2.1",
  "conteudo": "<p>Conteúdo completo do documento...</p>",
  "resumo": "Adicionamos a seção de assinaturas e pagamentos recorrentes.",
  "obrigatorio": true,
  "ativoDesdé": "2025-11-01T00:00:00Z"
}
```

**Response `404 Not Found`:**
```json
{
  "status": 404,
  "erro": "Nenhum documento ativo encontrado para o tipo TERMS_OF_USE."
}
```

**Response `400 Bad Request`** (tipo inválido):
```json
{
  "status": 400,
  "erro": "Tipo de documento inválido: 'OUTRO'. Valores aceitos: TERMS_OF_USE, PRIVACY_POLICY, COOKIE_POLICY."
}
```

---

### 6.2 Endpoints Autenticados — Usuário

---

#### `POST /legal/consentimentos`

Registra o aceite (ou recusa) do usuário para um ou mais documentos.  
**Uso:** chamado pelo frontend após o usuário clicar em "Aceitar" na modal de termos.

**Auth:** Bearer JWT  
**Headers:**  
- `Authorization: Bearer <token>`
- `X-Forwarded-For` (capturado automaticamente pelo backend para registrar o IP)

**Request body:**
```json
{
  "consentimentos": [
    {
      "documentoLegalId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "aceito": true
    },
    {
      "documentoLegalId": "4ab96g75-6828-5673-c4gd-3d074g77bgb7",
      "aceito": true
    },
    {
      "documentoLegalId": "5bc07h86-7939-6784-d5he-4e185h88chc8",
      "aceito": false
    }
  ]
}
```

**Validações:**
- `documentoLegalId` — obrigatório, deve existir e estar com status `ativo`
- `aceito` — obrigatório (não pode ser nulo; `false` é uma recusa explícita válida)
- Não é possível recusar um documento com `obrigatorio = true` (retorna `422`)

**Response `200 OK`:**
```json
{
  "consentimentos": [
    {
      "id": "aaa00000-0000-0000-0000-000000000001",
      "documentoLegalId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "tipo": "TERMS_OF_USE",
      "versao": "2.1.0",
      "aceito": true,
      "registradoEm": "2025-11-10T14:22:33Z"
    },
    {
      "id": "bbb00000-0000-0000-0000-000000000002",
      "documentoLegalId": "4ab96g75-6828-5673-c4gd-3d074g77bgb7",
      "tipo": "PRIVACY_POLICY",
      "versao": "1.3.0",
      "aceito": true,
      "registradoEm": "2025-11-10T14:22:33Z"
    },
    {
      "id": "ccc00000-0000-0000-0000-000000000003",
      "documentoLegalId": "5bc07h86-7939-6784-d5he-4e185h88chc8",
      "tipo": "COOKIE_POLICY",
      "versao": "1.0.0",
      "aceito": false,
      "registradoEm": "2025-11-10T14:22:33Z"
    }
  ]
}
```

**Response `422 Unprocessable Entity`** (tentativa de recusar documento obrigatório):
```json
{
  "status": 422,
  "erro": "O documento 'Termos de Uso — versão 2.1' é obrigatório e não pode ser recusado."
}
```

---

#### `GET /legal/consentimentos/status`

Retorna o status de consentimento do usuário autenticado para todos os documentos ativos.  
**Uso:** o frontend usa este endpoint para decidir se deve exibir a modal de termos ou bloquear o acesso.

**Auth:** Bearer JWT

**Response `200 OK`:**
```json
{
  "pendentes": [
    {
      "documentoLegalId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "tipo": "TERMS_OF_USE",
      "versao": "2.1.0",
      "titulo": "Termos de Uso — versão 2.1",
      "obrigatorio": true
    }
  ],
  "aceitos": [
    {
      "documentoLegalId": "4ab96g75-6828-5673-c4gd-3d074g77bgb7",
      "tipo": "PRIVACY_POLICY",
      "versao": "1.3.0",
      "titulo": "Política de Privacidade — versão 1.3",
      "aceito": true,
      "registradoEm": "2025-09-20T10:00:00Z"
    }
  ],
  "bloqueado": true
}
```

> `bloqueado: true` significa que existe ao menos um documento obrigatório pendente de aceite.  
> O frontend deve bloquear a navegação e exibir a modal de termos quando `bloqueado = true`.

---

#### `GET /legal/consentimentos/historico`

Retorna o histórico completo de consentimentos do usuário (todas as versões, todos os tipos).

**Auth:** Bearer JWT

**Response `200 OK`:**
```json
{
  "historico": [
    {
      "id": "aaa00000-0000-0000-0000-000000000001",
      "tipo": "TERMS_OF_USE",
      "versao": "2.1.0",
      "titulo": "Termos de Uso — versão 2.1",
      "aceito": true,
      "registradoEm": "2025-11-10T14:22:33Z"
    },
    {
      "id": "zzz00000-0000-0000-0000-000000000099",
      "tipo": "TERMS_OF_USE",
      "versao": "2.0.0",
      "titulo": "Termos de Uso — versão 2.0",
      "aceito": true,
      "registradoEm": "2025-06-01T09:00:00Z"
    }
  ]
}
```

---

### 6.3 Endpoints Administrativos

> Todos os endpoints abaixo exigem `Role.owner` ou `Role.admin` no token JWT.

---

#### `POST /admin/legal/documentos`

Cria um novo documento legal em rascunho. Não é publicado imediatamente.

**Auth:** Bearer JWT + Role `admin` ou `owner`

**Request body:**
```json
{
  "tipo": "TERMS_OF_USE",
  "versao": "2.2.0",
  "titulo": "Termos de Uso — versão 2.2",
  "conteudo": "<p>Conteúdo completo do novo documento...</p>",
  "resumo": "Atualização das condições de cancelamento de plano.",
  "obrigatorio": true
}
```

**Validações:**
- `tipo` — obrigatório, deve ser um dos valores do enum `TipoDocumentoLegal`
- `versao` — obrigatório, formato semver recomendado (`\d+\.\d+\.\d+`), deve ser única para o tipo
- `titulo` — obrigatório, máx. 255 caracteres
- `conteudo` — obrigatório, não pode estar em branco
- `resumo` — opcional
- `obrigatorio` — obrigatório (boolean)

**Response `201 Created`:**
```json
{
  "id": "9fa00000-0000-0000-0000-000000000001",
  "tipo": "TERMS_OF_USE",
  "versao": "2.2.0",
  "titulo": "Termos de Uso — versão 2.2",
  "status": "rascunho",
  "obrigatorio": true,
  "criadoEm": "2025-12-01T10:00:00Z"
}
```

**Response `409 Conflict`** (versão duplicada):
```json
{
  "status": 409,
  "erro": "Já existe um documento do tipo TERMS_OF_USE com a versão '2.2.0'."
}
```

---

#### `POST /admin/legal/documentos/{id}/publicar`

Publica (ativa) um documento em rascunho. O documento ativo anterior do mesmo tipo é automaticamente marcado como `substituido`.  
Esta operação é transacional e usa lock pessimista para evitar race conditions.

**Auth:** Bearer JWT + Role `admin` ou `owner`

**Response `200 OK`:**
```json
{
  "id": "9fa00000-0000-0000-0000-000000000001",
  "tipo": "TERMS_OF_USE",
  "versao": "2.2.0",
  "titulo": "Termos de Uso — versão 2.2",
  "status": "ativo",
  "ativoDesdé": "2025-12-01T11:30:00Z",
  "versaoAnteriorSubstituida": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "versao": "2.1.0",
    "substituidoEm": "2025-12-01T11:30:00Z"
  }
}
```

**Response `400 Bad Request`** (documento não está em rascunho):
```json
{
  "status": 400,
  "erro": "Apenas documentos com status 'rascunho' podem ser publicados. Status atual: 'ativo'."
}
```

---

#### `PUT /admin/legal/documentos/{id}`

Atualiza o conteúdo de um documento **apenas enquanto em rascunho**.  
Documentos `ativo` ou `substituido` são imutáveis.

**Auth:** Bearer JWT + Role `admin` ou `owner`

**Request body:**
```json
{
  "titulo": "Termos de Uso — versão 2.2 (revisado)",
  "conteudo": "<p>Conteúdo corrigido...</p>",
  "resumo": "Correção de erros tipográficos.",
  "obrigatorio": true
}
```

**Response `200 OK`:** mesma estrutura do `POST /admin/legal/documentos`

**Response `409 Conflict`** (documento não está em rascunho):
```json
{
  "status": 409,
  "erro": "Documentos publicados ou substituídos não podem ser editados. Crie uma nova versão."
}
```

---

#### `GET /admin/legal/documentos`

Lista todas as versões de todos os documentos com paginação e filtros.

**Auth:** Bearer JWT + Role `admin` ou `owner`

**Query params:**
| Param  | Tipo    | Padrão  | Descrição                                             |
|--------|---------|---------|-------------------------------------------------------|
| tipo   | string  | —       | Filtra por tipo (`TERMS_OF_USE`, `PRIVACY_POLICY`…)  |
| status | string  | —       | Filtra por status (`rascunho`, `ativo`, `substituido`)|
| page   | integer | 0       | Página (0-based)                                      |
| size   | integer | 20      | Itens por página (máx. 100)                           |

**Response `200 OK`:**
```json
{
  "conteudo": [
    {
      "id": "9fa00000-0000-0000-0000-000000000001",
      "tipo": "TERMS_OF_USE",
      "versao": "2.2.0",
      "titulo": "Termos de Uso — versão 2.2",
      "status": "rascunho",
      "obrigatorio": true,
      "criadoEm": "2025-12-01T10:00:00Z",
      "ativoDesdé": null,
      "criadoPor": {
        "id": "usr-uuid",
        "nome": "Guilherme Mendes"
      }
    }
  ],
  "pagina": 0,
  "tamanhoPagina": 20,
  "totalItens": 7,
  "totalPaginas": 1
}
```

---

#### `GET /admin/legal/documentos/{id}`

Busca um documento específico pelo ID com todos os campos.

**Auth:** Bearer JWT + Role `admin` ou `owner`

**Response `200 OK`:**
```json
{
  "id": "9fa00000-0000-0000-0000-000000000001",
  "tipo": "TERMS_OF_USE",
  "versao": "2.2.0",
  "titulo": "Termos de Uso — versão 2.2",
  "conteudo": "<p>...</p>",
  "resumo": "Atualização das condições de cancelamento.",
  "status": "rascunho",
  "obrigatorio": true,
  "criadoEm": "2025-12-01T10:00:00Z",
  "atualizadoEm": "2025-12-01T10:45:00Z",
  "ativoDesdé": null,
  "substituidoEm": null,
  "criadoPor": {
    "id": "usr-uuid",
    "nome": "Guilherme Mendes"
  },
  "atualizadoPor": {
    "id": "usr-uuid",
    "nome": "Guilherme Mendes"
  }
}
```

---

#### `DELETE /admin/legal/documentos/{id}`

Soft-delete de um documento (somente `rascunho`). Documentos `ativo` ou `substituido` não podem ser deletados.

**Auth:** Bearer JWT + Role `owner`

**Response `204 No Content`**

**Response `409 Conflict`:**
```json
{
  "status": 409,
  "erro": "Não é possível excluir um documento ativo ou substituído."
}
```

---

#### `GET /admin/legal/consentimentos/relatorio`

Relatório de adesão — quantos usuários aceitaram cada documento ativo.

**Auth:** Bearer JWT + Role `owner`

**Response `200 OK`:**
```json
{
  "relatorio": [
    {
      "documentoLegalId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "tipo": "TERMS_OF_USE",
      "versao": "2.1.0",
      "titulo": "Termos de Uso — versão 2.1",
      "totalUsuarios": 342,
      "totalAceites": 318,
      "totalRecusas": 24,
      "percentualAdesao": 92.98
    }
  ]
}
```

---

## 7. Regras de Negócio

| # | Regra |
|---|-------|
| 1 | Apenas um documento pode estar `ativo` por tipo simultaneamente. A publicação de um novo ativa o lock pessimista e marca o anterior como `substituido`. |
| 2 | Documentos `ativo` ou `substituido` são **imutáveis** — qualquer edição deve ser uma nova versão em rascunho. |
| 3 | A `versao` é única por `tipo` — não pode repetir nem em documentos excluídos logicamente. |
| 4 | O consentimento é **imutável** — cada aceite ou recusa gera um novo registro. Re-aceitar uma versão executa upsert via `ON CONFLICT DO UPDATE` apenas no campo `aceito` e `criado_em`. |
| 5 | Documentos com `obrigatorio = true` **não podem ser recusados**. Tentativa retorna `422`. |
| 6 | O endpoint `GET /legal/consentimentos/status` é o único responsável por informar ao frontend se o acesso deve ser bloqueado (`bloqueado: true`). O bloqueio de rota deve ser feito no frontend com base nesta resposta. |
| 7 | O backend **não bloqueia** requests de API por falta de consentimento — isso é responsabilidade do frontend. Exceção: pode-se adicionar um filtro Spring Security opcional que verifica o status e retorna `403` com body estruturado. |
| 8 | IP e user-agent são capturados automaticamente via `HttpServletRequest` no controller — não devem ser enviados pelo cliente. |
| 9 | Audit log deve ser registrado em `audit_logs` para: criação, publicação, edição e exclusão de documentos legais. |
| 10 | Soft delete padrão: `excluido_em IS NOT NULL` exclui o documento de todas as queries. |

---

## 8. Fluxo de Uso — Frontend

```
1. App inicializa
   └── GET /legal/documentos/vigentes           → carrega os documentos atuais para exibir
   └── GET /legal/consentimentos/status          → verifica se usuário tem pendências
         ├── bloqueado: false → acesso liberado
         └── bloqueado: true  → exibe modal com os documentos pendentes
               └── usuário clica "Aceitar"
                     └── POST /legal/consentimentos  → registra aceite
                           └── GET /legal/consentimentos/status → confirma desbloqueio
```

---

## 9. Fluxo Admin — Publicar Nova Versão

```
1. Admin acessa painel de documentos legais
   └── GET /admin/legal/documentos?tipo=TERMS_OF_USE

2. Admin cria rascunho da nova versão
   └── POST /admin/legal/documentos
         └── status: rascunho

3. Admin revisa e edita se necessário
   └── PUT /admin/legal/documentos/{id}

4. Admin publica
   └── POST /admin/legal/documentos/{id}/publicar
         ├── versão anterior → status: substituido
         └── nova versão     → status: ativo

5. Frontend detecta nova versão na próxima chamada de /legal/consentimentos/status
   └── usuários que não aceitaram a nova versão recebem bloqueado: true
```

---

## 10. Estrutura de Pacotes

```
com.api.konditor
├── app
│   └── controller
│       ├── LegalPublicoController.java          # GET /legal/*  (sem auth)
│       ├── LegalUsuarioController.java          # POST|GET /legal/consentimentos/* (autenticado)
│       └── admin
│           └── LegalAdminController.java        # /admin/legal/* (role admin/owner)
│   └── controller
│       ├── request
│       │   ├── CriarDocumentoLegalRequest.java
│       │   ├── AtualizarDocumentoLegalRequest.java
│       │   └── RegistrarConsentimentosRequest.java
│       └── response
│           ├── DocumentoLegalResponse.java
│           ├── DocumentoLegalDetalheResponse.java
│           ├── DocumentoLegalListResponse.java
│           ├── ConsentimentoResponse.java
│           ├── ConsentimentoStatusResponse.java
│           ├── ConsentimentoHistoricoResponse.java
│           └── RelatorioAdesaoResponse.java
│
├── domain
│   ├── enuns
│   │   ├── TipoDocumentoLegal.java
│   │   ├── StatusDocumentoLegal.java
│   │   └── MetodoAceite.java
│   └── usecase
│       ├── DocumentoLegalUseCase.java           # interface
│       └── impl
│           └── DocumentoLegalUseCaseImpl.java
│
└── infra
    └── jpa
        ├── entity
        │   ├── DocumentoLegalJpaEntity.java
        │   └── ConsentimentoUsuarioJpaEntity.java
        └── repository
            ├── DocumentoLegalJpaRepository.java
            └── ConsentimentoUsuarioJpaRepository.java
```

---

## 11. Migration SQL (Flyway)

Criar o arquivo `V6__termos_legais.sql` com o DDL completo da seção 3.

---

## 12. Critérios de Aceitação

- [ ] `GET /legal/documentos/vigentes` retorna os 3 tipos ativos sem autenticação
- [ ] `GET /legal/documentos/{tipo}` retorna `404` quando não há documento ativo
- [ ] `POST /legal/consentimentos` registra aceite e retorna os consentimentos salvos
- [ ] `POST /legal/consentimentos` retorna `422` ao tentar recusar documento obrigatório
- [ ] `GET /legal/consentimentos/status` retorna `bloqueado: true` quando há documentos obrigatórios pendentes
- [ ] `POST /admin/legal/documentos/{id}/publicar` substitui o documento ativo anterior atomicamente
- [ ] `PUT /admin/legal/documentos/{id}` retorna `409` para documentos não-rascunho
- [ ] `DELETE /admin/legal/documentos/{id}` retorna `409` para documentos não-rascunho
- [ ] Nenhum endpoint de escrita aceita requisição sem autenticação válida
- [ ] IP e user-agent são registrados automaticamente no consentimento
- [ ] Audit log registrado para criação, publicação, edição e exclusão de documentos
- [ ] Testes unitários com cobertura de todos os casos de negócio

---

## 13. Considerações de Segurança e LGPD

- O conteúdo do documento (`conteudo`) pode conter HTML — sanitizar antes de persistir usando `Jsoup.clean()` com whitelist permissiva para documentos legais.
- Os registros de `consentimentos_usuario` devem ser retidos pelo prazo legal (recomendação: 5 anos após encerramento do contrato com o usuário) — não devem ter soft delete.
- Em caso de solicitação de exclusão de dados (LGPD Art. 18, inciso VI), o sistema deve anonimizar o `usuario_id` nos consentimentos em vez de excluir os registros, preservando a evidência do aceite.
- O endpoint `GET /legal/documentos/vigentes` deve ter cache HTTP (`Cache-Control: public, max-age=3600`) já que o conteúdo raramente muda.
