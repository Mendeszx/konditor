-- =============================================================================
-- KONDITOR — Schema completo (V1 + V2 + V3 unificados)
-- Doceria multi-tenant: workspaces, receitas, pedidos, financeiro
-- =============================================================================

-- EXTENSIONS
create extension if not exists "pgcrypto";

-- =============================================================================
-- TABELAS DE DOMÍNIO AUXILIAR (enums ricos)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Papéis de membro dentro de um workspace
-- ---------------------------------------------------------------------------
create table roles (
  name            text    primary key,
  display_name    text    not null,
  description     text,
  hierarchy_level integer not null,

  -- Gerenciamento do workspace
  can_manage_workspace  boolean not null,
  can_manage_members    boolean not null,
  can_manage_plan       boolean not null,

  -- Ingredientes
  can_create_ingredients boolean not null,
  can_edit_ingredients   boolean not null,
  can_delete_ingredients boolean not null,

  -- Produtos / Receitas
  can_create_products    boolean not null,
  can_edit_products      boolean not null,
  can_delete_products    boolean not null,

  -- Pedidos
  can_create_orders      boolean not null,
  can_edit_orders        boolean not null,
  can_delete_orders      boolean not null,

  -- Financeiro
  can_view_reports       boolean not null,
  can_view_costs         boolean not null,

  -- Auditoria
  can_view_audit_log     boolean not null,

  is_active  boolean     not null,
  created_at timestamptz not null default now()
);

insert into roles
  (name, display_name, description, hierarchy_level,
   can_manage_workspace, can_manage_members, can_manage_plan,
   can_create_ingredients, can_edit_ingredients, can_delete_ingredients,
   can_create_products,   can_edit_products,   can_delete_products,
   can_create_orders,     can_edit_orders,     can_delete_orders,
   can_view_reports, can_view_costs, can_view_audit_log, is_active)
values
  ('owner',  'Proprietário',  'Criador do workspace. Acesso completo a todos os recursos.',
   100, true,  true,  true,
        true,  true,  true,
        true,  true,  true,
        true,  true,  true,
        true,  true,  true,  true),
  ('admin',  'Administrador', 'Gestão operacional completa. Não pode excluir o workspace nem alterar o plano.',
   50,  false, true,  false,
        true,  true,  true,
        true,  true,  true,
        true,  true,  true,
        true,  true,  false, true),
  ('member', 'Membro',        'Cria e edita ingredientes, produtos e pedidos. Não acessa financeiro.',
   10,  false, false, false,
        true,  true,  false,
        true,  true,  false,
        true,  true,  false,
        false, false, false, true);

-- ---------------------------------------------------------------------------
-- Planos de assinatura com todos os detalhes comerciais e limites
-- ---------------------------------------------------------------------------
create table plan_details (
  id           uuid primary key default gen_random_uuid(),
  name         text not null unique,
  display_name text not null,
  description  text,

  -- Preço
  price_cents   integer not null,
  billing_cycle text    not null,  -- monthly | yearly | once — validado no useCase

  -- Limites de uso (null = ilimitado)
  max_workspaces        integer,
  max_members           integer,
  max_products          integer,
  max_ingredients       integer,
  max_orders_per_month  integer,

  -- Funcionalidades inclusas
  has_cost_calculation boolean not null,
  has_order_management boolean not null,
  has_reports          boolean not null,
  has_audit_log        boolean not null,
  has_api_access       boolean not null,
  has_priority_support boolean not null,

  is_active  boolean     not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz
);

insert into plan_details
  (name, display_name, description, price_cents, billing_cycle,
   max_workspaces, max_members, max_products, max_ingredients, max_orders_per_month,
   has_cost_calculation, has_order_management, has_reports, has_audit_log, has_api_access, has_priority_support,
   is_active)
values
  ('free',    'Gratuito', 'Plano gratuito com recursos básicos de cadastro.',
   0,    'monthly', 1, 1,  10,   20,   20,   true,  false, false, false, false, false, true),
  ('basic',   'Básico',   'Para doceiros que estão começando a crescer.',
   2990, 'monthly', 1, 3,  50,   100,  100,  true,  true,  false, false, false, false, true),
  ('premium', 'Premium',  'Para doceiros profissionais. Sem limites operacionais.',
   6990, 'monthly', 3, 10, null, null, null, true,  true,  true,  true,  true,  true,  true);

-- ---------------------------------------------------------------------------
-- Tipos de unidade de medida
-- ---------------------------------------------------------------------------
create table unit_types (
  name         text primary key,
  display_name text not null,
  description  text
);
insert into unit_types (name, display_name, description) values
  ('weight', 'Peso',   'Gramas, quilogramas, etc.'),
  ('volume', 'Volume', 'Mililitros, litros, etc.'),
  ('unit',   'Unidade','Peças, dúzias, etc.');

-- ---------------------------------------------------------------------------
-- Status de pedido
-- ---------------------------------------------------------------------------
create table order_statuses (
  name         text primary key,
  display_name text not null,
  description  text,
  sort_order   integer not null
);
insert into order_statuses (name, display_name, description, sort_order) values
  ('pending',   'Aguardando',  'Pedido recebido, aguardando confirmação.',  1),
  ('confirmed', 'Confirmado',  'Pedido confirmado, em produção.',           2),
  ('ready',     'Pronto',      'Produto pronto para entrega/retirada.',     3),
  ('delivered', 'Entregue',    'Pedido entregue ao cliente.',               4),
  ('paid',      'Pago',        'Pagamento confirmado.',                     5),
  ('cancelled', 'Cancelado',   'Pedido cancelado.',                         6);

-- ---------------------------------------------------------------------------
-- Status de assinatura
-- ---------------------------------------------------------------------------
create table subscription_statuses (
  name         text primary key,
  display_name text not null,
  description  text
);
insert into subscription_statuses (name, display_name, description) values
  ('active',    'Ativa',     'Assinatura vigente.'),
  ('trialing',  'Trial',     'Período de avaliação gratuita.'),
  ('past_due',  'Em atraso', 'Pagamento vencido, acesso temporariamente mantido.'),
  ('inactive',  'Inativa',   'Assinatura suspensa.'),
  ('cancelled', 'Cancelada', 'Assinatura cancelada pelo usuário.');

-- ---------------------------------------------------------------------------
-- Operações de auditoria
-- ---------------------------------------------------------------------------
create table audit_operations (
  name         text primary key,
  display_name text not null
);
insert into audit_operations (name, display_name) values
  ('CREATE', 'Criação'),
  ('UPDATE', 'Atualização'),
  ('DELETE', 'Exclusão');

-- =============================================================================
-- USUÁRIOS
-- =============================================================================
create table users (
  id         uuid primary key default gen_random_uuid(),
  email      text not null unique,
  name       text,
  google_id  text unique,
  locale     text,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- WORKSPACES (MULTI-TENANT)
-- =============================================================================
create table workspaces (
  id         uuid primary key default gen_random_uuid(),
  name       text not null,
  owner_id   uuid not null references users(id),
  plan_id    uuid not null references plan_details(id),
  currency   text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- MEMBROS DO WORKSPACE
-- =============================================================================
create table workspace_members (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id) on delete cascade,
  user_id      uuid not null references users(id) on delete cascade,
  role         text not null references roles(name),
  invited_by   uuid references users(id),
  joined_at    timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz,
  deleted_at   timestamptz,
  created_by   uuid references users(id),
  updated_by   uuid references users(id),
  unique (workspace_id, user_id)
);

-- =============================================================================
-- CATEGORIAS DE PRODUTO — tabela global (ex: Bolo, Brigadeiro, Salgado)
-- Compartilhada entre todos os workspaces; não possui workspace_id.
-- =============================================================================
create table product_categories (
  id         uuid primary key default gen_random_uuid(),
  name       text not null,
  color      text,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);
create unique index idx_product_categories_name
    on product_categories(lower(name))
    where deleted_at is null;

-- =============================================================================
-- UNIDADES DE MEDIDA
-- =============================================================================
create table units (
  id         uuid primary key default gen_random_uuid(),
  name       text not null,
  symbol     text not null unique,
  type       text not null references unit_types(name),
  is_base    boolean not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- CONVERSÕES DE UNIDADE
-- =============================================================================
create table unit_conversions (
  id           uuid primary key default gen_random_uuid(),
  from_unit_id uuid    not null references units(id),
  to_unit_id   uuid    not null references units(id),
  factor       numeric not null,  -- deve ser > 0 — validado no useCase
  created_at   timestamptz not null default now(),
  updated_at   timestamptz,
  deleted_at   timestamptz,
  created_by   uuid references users(id),
  updated_by   uuid references users(id),
  unique (from_unit_id, to_unit_id)
  -- from_unit_id <> to_unit_id — validado no useCase
);

-- =============================================================================
-- CATEGORIAS DE INGREDIENTE — tabela global (ex: Chocolate, Laticínio, Farinha)
-- Compartilhada entre todos os workspaces; não possui workspace_id.
-- =============================================================================
create table ingredient_categories (
  id         uuid        primary key default gen_random_uuid(),
  name       text        not null,
  color      text,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid        references users(id),
  updated_by uuid        references users(id)
);

-- =============================================================================
-- INGREDIENTES
-- =============================================================================
create table ingredients (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id) on delete cascade,
  name         text not null,
  brand        text,
  unit_id      uuid not null references units(id),

  -- V3: código e descrição opcionais
  code         text,
  description  text,
  category_id  uuid references ingredient_categories(id) on delete set null,

  cost_per_unit     numeric not null,  -- deve ser >= 0 — validado no useCase
  stock_quantity    numeric,           -- null = não controla estoque
  stock_alert_min   numeric,           -- deve ser >= 0 — validado no useCase

  notes        text,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz,
  deleted_at   timestamptz,
  created_by   uuid references users(id),
  updated_by   uuid references users(id)
);
create unique index idx_ingredients_workspace_name on ingredients(workspace_id, lower(name));

-- =============================================================================
-- HISTÓRICO DE PREÇO DE INGREDIENTES (V3)
-- =============================================================================
create table ingredient_price_history (
  id            uuid        primary key default gen_random_uuid(),
  ingredient_id uuid        not null references ingredients(id) on delete cascade,
  old_price     numeric     not null,
  new_price     numeric     not null,
  changed_at    timestamptz not null default now(),
  changed_by    uuid        references users(id)
);

-- =============================================================================
-- PRODUTOS
-- =============================================================================
create table products (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id) on delete cascade,
  category_id  uuid references product_categories(id) on delete set null,
  name         text not null,
  description  text,

  selling_price     numeric not null,  -- deve ser >= 0 — validado no useCase
  yield_quantity    numeric not null,  -- deve ser > 0  — validado no useCase
  yield_unit_id     uuid references units(id),
  calculated_cost   numeric,           -- deve ser >= 0 — validado no useCase
  prep_time_minutes integer,           -- deve ser >= 0 — validado no useCase

  -- V2: status da receita e preço sugerido
  status          text    not null default 'publicada'
                          constraint chk_products_status check (status in ('rascunho', 'publicada')),
  suggested_price numeric,

  notes      text,
  is_active  boolean not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);
create unique index idx_products_workspace_name on products(workspace_id, lower(name));

-- =============================================================================
-- RECEITA (INGREDIENTES DO PRODUTO)
-- =============================================================================
create table product_ingredients (
  id            uuid    primary key default gen_random_uuid(),
  product_id    uuid    not null references products(id) on delete cascade,
  ingredient_id uuid    not null references ingredients(id),
  quantity      numeric not null,  -- deve ser > 0 — validado no useCase
  unit_id       uuid    not null references units(id),
  notes         text,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz,
  deleted_at    timestamptz,
  created_by    uuid references users(id),
  updated_by    uuid references users(id),
  unique (product_id, ingredient_id)
);

-- =============================================================================
-- PEDIDOS
-- =============================================================================
create table orders (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id) on delete cascade,

  client_name  text,
  client_phone text,

  status        text not null references order_statuses(name),
  delivery_date date,
  delivery_time time,
  is_delivery   boolean not null,

  delivery_address text,

  total_price    numeric,   -- deve ser >= 0 — validado no useCase
  discount_cents integer not null,  -- deve ser >= 0 — validado no useCase
  notes          text,

  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- ITENS DO PEDIDO
-- =============================================================================
create table order_items (
  id         uuid    primary key default gen_random_uuid(),
  order_id   uuid    not null references orders(id) on delete cascade,
  product_id uuid    not null references products(id),
  quantity   numeric not null,  -- deve ser > 0 — validado no useCase

  unit_price_at_time numeric not null,  -- deve ser >= 0 — validado no useCase

  notes      text,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- REFRESH TOKENS
-- =============================================================================
create table refresh_tokens (
  id         uuid primary key default gen_random_uuid(),
  token      text not null unique,
  user_id    uuid not null references users(id) on delete cascade,
  expires_at timestamptz not null,
  revoked    boolean not null,
  created_at timestamptz not null default now()
);

-- =============================================================================
-- ASSINATURAS
-- =============================================================================
create table subscriptions (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id) on delete cascade,
  plan_id      uuid not null references plan_details(id),
  status       text not null references subscription_statuses(name),

  started_at    timestamptz,
  ends_at       timestamptz,
  trial_ends_at timestamptz,

  external_subscription_id text unique,
  last_payment_at          timestamptz,
  next_billing_at          timestamptz,

  created_at timestamptz not null default now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  created_by uuid references users(id),
  updated_by uuid references users(id)
);

-- =============================================================================
-- AUDIT LOG
-- =============================================================================
create table audit_logs (
  id           uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references workspaces(id),
  entity_name  text not null,
  entity_id    uuid not null,
  operation    text not null references audit_operations(name),
  data_before  jsonb,
  data_after   jsonb,
  changed_fields jsonb,
  performed_by uuid references users(id),
  performed_at timestamptz not null default now(),
  request_id   text,
  ip_address   text,
  created_at   timestamptz not null default now()
);

-- =============================================================================
-- INDEXES
-- =============================================================================

-- Busca ativa (soft-delete)
create index idx_users_deleted_at            on users(deleted_at)             where deleted_at is null;
create index idx_workspaces_deleted_at        on workspaces(deleted_at)        where deleted_at is null;
create index idx_ingredients_deleted_at       on ingredients(deleted_at)       where deleted_at is null;
create index idx_products_deleted_at          on products(deleted_at)          where deleted_at is null;
create index idx_orders_deleted_at            on orders(deleted_at)            where deleted_at is null;

-- Chaves de relacionamento mais usadas
create index idx_workspaces_owner             on workspaces(owner_id);
create index idx_workspace_members_workspace  on workspace_members(workspace_id);
create index idx_workspace_members_user       on workspace_members(user_id);
create index idx_products_workspace           on products(workspace_id);
create index idx_products_category            on products(category_id);
create index idx_ingredients_workspace        on ingredients(workspace_id);
create index idx_orders_workspace             on orders(workspace_id);
create index idx_orders_status                on orders(status);
create index idx_orders_delivery_date         on orders(delivery_date);
create index idx_order_items_order            on order_items(order_id);
create index idx_product_ingredients_product  on product_ingredients(product_id);
create index idx_subscriptions_workspace      on subscriptions(workspace_id);

-- Tokens
create index idx_refresh_token_value          on refresh_tokens(token);
create index idx_refresh_token_user           on refresh_tokens(user_id);

-- Auditoria
create index idx_audit_entity                 on audit_logs(entity_name, entity_id);
create index idx_audit_workspace              on audit_logs(workspace_id);
create index idx_audit_performed_at           on audit_logs(performed_at);

-- V2: receitas publicadas (query mais comum no dashboard)
create index idx_products_workspace_status
    on products(workspace_id, status)
    where deleted_at is null and is_active = true;

-- V3: categorias de ingrediente (globais — sem workspace_id)
create unique index idx_ingredient_categories_name
    on ingredient_categories(lower(name))
    where deleted_at is null;

create index idx_ingredient_categories_active
    on ingredient_categories(id)
    where deleted_at is null;

-- V3: código único de ingrediente por workspace (ignora NULLs)
create unique index idx_ingredients_workspace_code
    on ingredients(workspace_id, code)
    where code is not null and deleted_at is null;

create index idx_ingredients_category
    on ingredients(category_id)
    where deleted_at is null;

-- V3: histórico de preço
create index idx_ingredient_price_history_ingredient
    on ingredient_price_history(ingredient_id, changed_at desc);

-- =============================================================================
-- SEED — Unidades de medida globais
-- =============================================================================

insert into units (name, symbol, type, is_base) values
  -- Peso
  ('Grama',       'g',   'weight', true),
  ('Quilograma',  'kg',  'weight', false),
  ('Miligrama',   'mg',  'weight', false),
  -- Volume
  ('Mililitro',   'ml',  'volume', true),
  ('Litro',       'L',   'volume', false),
  ('Colher de sopa', 'colher sopa', 'volume', false),
  ('Colher de chá',  'colher chá',  'volume', false),
  ('Xícara',      'xícara', 'volume', false),
  -- Contagem
  ('Unidade',     'un',  'unit', true),
  ('Dúzia',       'dz',  'unit', false),
  ('Pacote',      'pct', 'unit', false);

-- Conversões de unidade (peso)
insert into unit_conversions (from_unit_id, to_unit_id, factor)
select f.id, t.id, c.factor
from (values
  ('kg',  'g',  1000.000000),
  ('g',   'kg', 0.001000),
  ('mg',  'g',  0.001000),
  ('g',   'mg', 1000.000000)
) as c(from_sym, to_sym, factor)
join units f on f.symbol = c.from_sym
join units t on t.symbol = c.to_sym;

-- Conversões de unidade (volume)
insert into unit_conversions (from_unit_id, to_unit_id, factor)
select f.id, t.id, c.factor
from (values
  ('L',          'ml', 1000.000000),
  ('ml',         'L',  0.001000),
  ('colher sopa','ml', 15.000000),
  ('colher chá', 'ml', 5.000000),
  ('xícara',     'ml', 240.000000)
) as c(from_sym, to_sym, factor)
join units f on f.symbol = c.from_sym
join units t on t.symbol = c.to_sym;

-- Conversões de unidade (contagem)
insert into unit_conversions (from_unit_id, to_unit_id, factor)
select f.id, t.id, c.factor
from (values
  ('dz', 'un', 12.000000),
  ('un', 'dz', 0.083333)
) as c(from_sym, to_sym, factor)
join units f on f.symbol = c.from_sym
join units t on t.symbol = c.to_sym;

-- =============================================================================
-- SEED — Categorias de produto globais
-- =============================================================================

insert into product_categories (name, color) values
  ('Bolo',            '#F59E0B'),
  ('Brigadeiro',      '#92400E'),
  ('Cupcake',         '#EC4899'),
  ('Torta',           '#D97706'),
  ('Cookie',          '#78350F'),
  ('Salgado',         '#10B981'),
  ('Mousse',          '#6366F1'),
  ('Doce de Festa',   '#EF4444'),
  ('Sobremesa',       '#8B5CF6'),
  ('Pão de Mel',      '#B45309'),
  ('Trufa',           '#1F2937'),
  ('Macarons',        '#F472B6'),
  ('Outros',          '#9CA3AF');

-- =============================================================================
-- SEED — Categorias de ingrediente globais
-- =============================================================================

insert into ingredient_categories (name, color) values
  ('Chocolate',              '#78350F'),
  ('Laticínio',              '#FEF3C7'),
  ('Farinha e Grãos',        '#D97706'),
  ('Açúcar e Adoçantes',     '#FDE68A'),
  ('Gordura',                '#FBBF24'),
  ('Ovos',                   '#F59E0B'),
  ('Frutas e Polpas',        '#EF4444'),
  ('Aromatizante',           '#8B5CF6'),
  ('Fermento e Levedura',    '#6EE7B7'),
  ('Corante e Decoração',    '#EC4899'),
  ('Embalagem',              '#6B7280'),
  ('Castanhas e Sementes',   '#92400E'),
  ('Bebidas e Licores',      '#1D4ED8'),
  ('Conservante e Aditivo',  '#374151'),
  ('Outros',                 '#9CA3AF');


