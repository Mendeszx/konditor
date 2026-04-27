-- =============================================================================
-- KONDITOR — Schema completo unificado (V1 + V2 + V3 + V4)
-- Gerado em: 2026-04-18
--
-- V1 — Schema base: espacos_trabalho, usuarios, ingredientes, produtos, pedidos,
--      assinaturas, auditoria, unidades, conversoes e seeds globais.
-- V2 — Suporte a peso/volume por porção no produto (receita).
-- V3 — Parâmetros completos de custo por receita: mão de obra, custos fixos e margem desejada.
-- V4 — Cálculos aprimorados: custo/preço por unidade, por porção e por g/ml.
-- =============================================================================

-- EXTENSÕES
create extension if not exists "pgcrypto";

-- =============================================================================
-- SCHEMA
-- =============================================================================
create schema if not exists konditor;
set search_path to konditor, public;

-- =============================================================================
-- TABELAS DE DOMÍNIO / LOOKUP
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Papéis de membros do espaço de trabalho
-- ---------------------------------------------------------------------------
create table papeis (
  nome                text    primary key,
  nome_exibido        text    not null,
  descricao           text,
  nivel_hierarquia    integer not null,

  pode_gerenciar_espaco_trabalho  boolean not null,
  pode_gerenciar_membros          boolean not null,
  pode_gerenciar_plano            boolean not null,

  pode_criar_ingredientes   boolean not null,
  pode_editar_ingredientes  boolean not null,
  pode_excluir_ingredientes boolean not null,

  pode_criar_produtos   boolean not null,
  pode_editar_produtos  boolean not null,
  pode_excluir_produtos boolean not null,

  pode_criar_pedidos   boolean not null,
  pode_editar_pedidos  boolean not null,
  pode_excluir_pedidos boolean not null,

  pode_ver_relatorios    boolean not null,
  pode_ver_custos        boolean not null,
  pode_ver_log_auditoria boolean not null,

  ativo     boolean     not null,
  criado_em timestamptz not null default now()
);

insert into papeis
  (nome, nome_exibido, descricao, nivel_hierarquia,
   pode_gerenciar_espaco_trabalho, pode_gerenciar_membros, pode_gerenciar_plano,
   pode_criar_ingredientes, pode_editar_ingredientes, pode_excluir_ingredientes,
   pode_criar_produtos, pode_editar_produtos, pode_excluir_produtos,
   pode_criar_pedidos,  pode_editar_pedidos,  pode_excluir_pedidos,
   pode_ver_relatorios, pode_ver_custos, pode_ver_log_auditoria, ativo)
values
  ('owner',  'Proprietário',  'Criador do espaço de trabalho. Acesso completo a todos os recursos.',
   100, true,  true,  true,
        true,  true,  true,
        true,  true,  true,
        true,  true,  true,
        true,  true,  true,  true),
  ('admin',  'Administrador', 'Gestão operacional completa. Não pode excluir o espaço de trabalho nem alterar o plano.',
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
-- Planos de assinatura
-- ---------------------------------------------------------------------------
create table detalhes_plano (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null unique,
  nome_exibido  text not null,
  descricao     text,

  preco_centavos integer not null,
  ciclo_cobranca text    not null,  -- monthly | yearly | once

  max_espacos_trabalho integer,
  max_membros          integer,
  max_produtos         integer,
  max_ingredientes     integer,
  max_pedidos_por_mes  integer,

  tem_calculo_custos        boolean not null,
  tem_gerenciamento_pedidos boolean not null,
  tem_relatorios            boolean not null,
  tem_log_auditoria         boolean not null,
  tem_acesso_api            boolean not null,
  tem_suporte_prioritario   boolean not null,

  ativo         boolean     not null,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz
);

insert into detalhes_plano
  (nome, nome_exibido, descricao, preco_centavos, ciclo_cobranca,
   max_espacos_trabalho, max_membros, max_produtos, max_ingredientes, max_pedidos_por_mes,
   tem_calculo_custos, tem_gerenciamento_pedidos, tem_relatorios, tem_log_auditoria, tem_acesso_api, tem_suporte_prioritario,
   ativo)
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
create table tipos_unidade (
  nome         text primary key,
  nome_exibido text not null,
  descricao    text
);
insert into tipos_unidade (nome, nome_exibido, descricao) values
  ('weight', 'Peso',    'Gramas, quilogramas, etc.'),
  ('volume', 'Volume',  'Mililitros, litros, etc.'),
  ('unit',   'Unidade', 'Peças, dúzias, etc.');

-- ---------------------------------------------------------------------------
-- Status de pedido
-- ---------------------------------------------------------------------------
create table status_pedido (
  nome           text primary key,
  nome_exibido   text    not null,
  descricao      text,
  ordem_exibicao integer not null
);
insert into status_pedido (nome, nome_exibido, descricao, ordem_exibicao) values
  ('pending',   'Aguardando',  'Pedido recebido, aguardando confirmação.',  1),
  ('confirmed', 'Confirmado',  'Pedido confirmado, em produção.',           2),
  ('ready',     'Pronto',      'Produto pronto para entrega/retirada.',     3),
  ('delivered', 'Entregue',    'Pedido entregue ao cliente.',               4),
  ('paid',      'Pago',        'Pagamento confirmado.',                     5),
  ('cancelled', 'Cancelado',   'Pedido cancelado.',                         6);

-- ---------------------------------------------------------------------------
-- Status de assinatura
-- ---------------------------------------------------------------------------
create table status_assinatura (
  nome         text primary key,
  nome_exibido text not null,
  descricao    text
);
insert into status_assinatura (nome, nome_exibido, descricao) values
  ('active',    'Ativa',      'Assinatura vigente.'),
  ('trialing',  'Trial',      'Período de avaliação gratuita.'),
  ('past_due',  'Em atraso',  'Pagamento vencido, acesso temporariamente mantido.'),
  ('inactive',  'Inativa',    'Assinatura suspensa.'),
  ('cancelled', 'Cancelada',  'Assinatura cancelada pelo usuário.');

-- ---------------------------------------------------------------------------
-- Operações de auditoria
-- ---------------------------------------------------------------------------
create table operacoes_auditoria (
  nome         text primary key,
  nome_exibido text not null
);
insert into operacoes_auditoria (nome, nome_exibido) values
  ('CREATE', 'Criação'),
  ('UPDATE', 'Atualização'),
  ('DELETE', 'Exclusão');

-- =============================================================================
-- USUÁRIOS
-- =============================================================================
create table usuarios (
  id            uuid primary key default gen_random_uuid(),
  email         text not null unique,
  nome          text,
  id_google     text unique,
  idioma        text,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);

-- =============================================================================
-- ESPAÇOS DE TRABALHO (MULTI-TENANT)
-- =============================================================================
create table espacos_trabalho (
  id              uuid primary key default gen_random_uuid(),
  nome            text not null,
  proprietario_id uuid not null references usuarios(id),
  plano_id        uuid not null references detalhes_plano(id),
  moeda           text not null,
  criado_em       timestamptz not null default now(),
  atualizado_em   timestamptz,
  excluido_em     timestamptz,
  criado_por      uuid references usuarios(id),
  atualizado_por  uuid references usuarios(id)
);

-- =============================================================================
-- MEMBROS DO ESPAÇO DE TRABALHO
-- =============================================================================
create table membros_espaco_trabalho (
  id                 uuid primary key default gen_random_uuid(),
  espaco_trabalho_id uuid not null references espacos_trabalho(id) on delete cascade,
  usuario_id         uuid not null references usuarios(id) on delete cascade,
  papel              text not null references papeis(nome),
  convidado_por      uuid references usuarios(id),
  entrou_em          timestamptz,
  criado_em          timestamptz not null default now(),
  atualizado_em      timestamptz,
  excluido_em        timestamptz,
  criado_por         uuid references usuarios(id),
  atualizado_por     uuid references usuarios(id),
  unique (espaco_trabalho_id, usuario_id)
);

-- =============================================================================
-- CATEGORIAS DE PRODUTO — tabela global
-- =============================================================================
create table categorias_produto (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  cor           text,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);
create unique index idx_categorias_produto_nome
    on categorias_produto(lower(nome))
    where excluido_em is null;

-- =============================================================================
-- UNIDADES DE MEDIDA
-- =============================================================================
create table unidades (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  simbolo       text not null unique,
  tipo          text not null references tipos_unidade(nome),
  base          boolean not null,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);

-- =============================================================================
-- CONVERSÕES DE UNIDADE
-- =============================================================================
create table conversoes_unidade (
  id                 uuid primary key default gen_random_uuid(),
  unidade_origem_id  uuid    not null references unidades(id),
  unidade_destino_id uuid    not null references unidades(id),
  fator              numeric not null,
  criado_em          timestamptz not null default now(),
  atualizado_em      timestamptz,
  excluido_em        timestamptz,
  criado_por         uuid references usuarios(id),
  atualizado_por     uuid references usuarios(id),
  unique (unidade_origem_id, unidade_destino_id)
);

-- =============================================================================
-- CATEGORIAS DE INGREDIENTE — tabela global
-- =============================================================================
create table categorias_ingrediente (
  id            uuid        primary key default gen_random_uuid(),
  nome          text        not null,
  cor           text,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid        references usuarios(id),
  atualizado_por uuid       references usuarios(id)
);

-- =============================================================================
-- INGREDIENTES
-- =============================================================================
create table ingredientes (
  id                    uuid primary key default gen_random_uuid(),
  espaco_trabalho_id    uuid not null references espacos_trabalho(id) on delete cascade,
  nome                  text not null,
  marca                 text,
  unidade_id            uuid not null references unidades(id),
  codigo                text,
  descricao             text,
  categoria_id          uuid references categorias_ingrediente(id) on delete set null,

  custo_por_unidade     numeric not null,
  quantidade_estoque    numeric,
  alerta_minimo_estoque numeric,

  notas         text,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);
create unique index idx_ingredientes_espaco_nome on ingredientes(espaco_trabalho_id, lower(nome));

-- =============================================================================
-- HISTÓRICO DE PREÇO DO INGREDIENTE
-- =============================================================================
create table historico_preco_ingrediente (
  id             uuid        primary key default gen_random_uuid(),
  ingrediente_id uuid        not null references ingredientes(id) on delete cascade,
  preco_antigo   numeric     not null,
  preco_novo     numeric     not null,
  alterado_em    timestamptz not null default now(),
  alterado_por   uuid        references usuarios(id)
);

-- =============================================================================
-- PRODUTOS (RECEITAS)
-- =============================================================================
create table produtos (
  id                 uuid primary key default gen_random_uuid(),
  espaco_trabalho_id uuid not null references espacos_trabalho(id) on delete cascade,
  categoria_id       uuid references categorias_produto(id) on delete set null,
  nome               text not null,
  descricao          text,

  preco_venda           numeric not null,
  quantidade_rendimento numeric not null,
  unidade_rendimento_id uuid references unidades(id),
  custo_calculado       numeric,
  tempo_preparo_minutos integer,

  status         text not null default 'publicada'
                 constraint chk_produtos_status check (status in ('rascunho', 'publicada')),
  preco_sugerido numeric,

  notas text,
  ativo boolean not null,

  -- V2
  peso_por_unidade            numeric,
  unidade_peso_por_unidade_id uuid references unidades(id),

  -- V3
  custo_mao_de_obra_por_hora numeric(19,2),
  custo_mao_de_obra          numeric(19,4),
  valor_custos_fixos         numeric(19,2),
  tipo_custos_fixos          varchar(20),
  custos_fixos               numeric(19,4),
  margem_desejada            numeric(5,2),

  -- V4
  custo_ingredientes      numeric(19,4),
  custo_unitario          numeric(19,4),
  preco_sugerido_unitario numeric(19,4),
  quantidade_porcoes      numeric(19,4),
  custo_por_grama_ml      numeric(19,6),
  preco_por_grama_ml      numeric(19,6),
  custo_por_porcao        numeric(19,4),
  preco_por_porcao        numeric(19,4),

  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);
create unique index idx_produtos_espaco_nome on produtos(espaco_trabalho_id, lower(nome));

-- =============================================================================
-- INGREDIENTES DO PRODUTO (ITENS DA RECEITA)
-- =============================================================================
create table ingredientes_produto (
  id             uuid    primary key default gen_random_uuid(),
  produto_id     uuid    not null references produtos(id) on delete cascade,
  ingrediente_id uuid    not null references ingredientes(id),
  quantidade     numeric not null,
  unidade_id     uuid    not null references unidades(id),
  notas          text,
  criado_em      timestamptz not null default now(),
  atualizado_em  timestamptz,
  excluido_em    timestamptz,
  criado_por     uuid references usuarios(id),
  atualizado_por uuid references usuarios(id),
  unique (produto_id, ingrediente_id)
);

-- =============================================================================
-- RECEITAS COMO INGREDIENTE (V5)
-- Uma sub-receita usada como ingrediente em outra receita.
-- O custo por unidade é: precoFinal / rendimentoQuantidade da sub-receita.
-- =============================================================================
create table receitas_como_ingrediente (
  id                     uuid primary key default gen_random_uuid(),
  produto_id             uuid not null references produtos(id) on delete cascade,
  receita_ingrediente_id uuid not null references produtos(id),
  quantidade             numeric(19,4) not null,
  notas                  text,
  criado_em              timestamptz not null default now(),
  atualizado_em          timestamptz,
  excluido_em            timestamptz,
  criado_por             uuid references usuarios(id),
  atualizado_por         uuid references usuarios(id),
  constraint chk_rci_no_self_reference check (produto_id != receita_ingrediente_id),
  unique (produto_id, receita_ingrediente_id)
);

create index idx_receitas_como_ingrediente_produto on receitas_como_ingrediente(produto_id);
create index idx_receitas_como_ingrediente_sub     on receitas_como_ingrediente(receita_ingrediente_id);

-- =============================================================================
-- PEDIDOS
-- =============================================================================
create table pedidos (
  id                 uuid primary key default gen_random_uuid(),
  espaco_trabalho_id uuid not null references espacos_trabalho(id) on delete cascade,

  nome_cliente     text,
  telefone_cliente text,

  status           text not null references status_pedido(nome),
  data_entrega     date,
  hora_entrega     time,
  eh_entrega       boolean not null,

  endereco_entrega  text,

  preco_total       numeric,
  desconto_centavos integer not null,
  notas             text,

  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);

-- =============================================================================
-- ITENS DO PEDIDO
-- =============================================================================
create table itens_pedido (
  id          uuid    primary key default gen_random_uuid(),
  pedido_id   uuid    not null references pedidos(id) on delete cascade,
  produto_id  uuid    not null references produtos(id),
  quantidade  numeric not null,

  preco_unitario_na_epoca numeric not null,

  notas         text,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);

-- =============================================================================
-- TOKENS DE ATUALIZAÇÃO
-- =============================================================================
create table tokens_atualizacao (
  id         uuid primary key default gen_random_uuid(),
  token      text not null unique,
  usuario_id uuid not null references usuarios(id) on delete cascade,
  expira_em  timestamptz not null,
  revogado   boolean not null,
  criado_em  timestamptz not null default now()
);

-- =============================================================================
-- ASSINATURAS
-- =============================================================================
create table assinaturas (
  id                 uuid primary key default gen_random_uuid(),
  espaco_trabalho_id uuid not null references espacos_trabalho(id) on delete cascade,
  plano_id           uuid not null references detalhes_plano(id),
  status             text not null references status_assinatura(nome),

  iniciado_em         timestamptz,
  termina_em          timestamptz,
  trial_termina_em    timestamptz,

  id_assinatura_externo text unique,
  ultimo_pagamento_em   timestamptz,
  proxima_cobranca_em   timestamptz,

  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz,
  excluido_em   timestamptz,
  criado_por    uuid references usuarios(id),
  atualizado_por uuid references usuarios(id)
);

-- =============================================================================
-- LOGS DE AUDITORIA
-- =============================================================================
create table logs_auditoria (
  id                 uuid primary key default gen_random_uuid(),
  espaco_trabalho_id uuid not null references espacos_trabalho(id),
  nome_entidade      text not null,
  id_entidade        uuid not null,
  operacao           text not null references operacoes_auditoria(nome),
  dados_antes        jsonb,
  dados_depois       jsonb,
  campos_alterados   jsonb,
  realizado_por      uuid references usuarios(id),
  realizado_em       timestamptz not null default now(),
  id_requisicao      text,
  endereco_ip        text,
  criado_em          timestamptz not null default now()
);

-- =============================================================================
-- COMENTÁRIOS DE COLUNAS — produtos
-- =============================================================================
comment on column produtos.custo_calculado              is 'Custo total do lote (ingredientes + mão de obra + custos fixos). Recalculado a cada salvamento.';
comment on column produtos.preco_sugerido               is 'Preço de venda sugerido para o lote inteiro, calculado a partir do custo total e da margem desejada.';
comment on column produtos.peso_por_unidade             is 'Peso ou volume de cada unidade/porção individual (ex: 15 para "15g por brigadeiro").';
comment on column produtos.unidade_peso_por_unidade_id  is 'Unidade de peso_por_unidade (ex: g, ml). Deve ser compatível com unidade_rendimento_id.';
comment on column produtos.custo_mao_de_obra_por_hora   is 'Custo de mão de obra por hora informado no último salvamento da receita (R$/h).';
comment on column produtos.custo_mao_de_obra            is 'Custo de mão de obra calculado = custo_mao_de_obra_por_hora × (tempo_preparo_minutos / 60).';
comment on column produtos.valor_custos_fixos           is 'Valor dos custos fixos informado (percentual ou absoluto, conforme tipo_custos_fixos).';
comment on column produtos.tipo_custos_fixos            is 'Tipo de custo fixo: "percentual" (% sobre custo de ingredientes) ou "fixo" (valor absoluto em R$).';
comment on column produtos.custos_fixos                 is 'Custos fixos calculados e armazenados no último salvamento.';
comment on column produtos.margem_desejada              is 'Margem de lucro desejada (%) usada para calcular preco_sugerido. Padrão: 30.';
comment on column produtos.custo_ingredientes           is 'Custo total dos ingredientes do lote (Σ custo_por_unidade × quantidade × fator_conversao).';
comment on column produtos.custo_unitario               is 'Custo total do lote dividido pelo quantidade_rendimento.';
comment on column produtos.preco_sugerido_unitario      is 'Preço sugerido do lote dividido pelo quantidade_rendimento.';
comment on column produtos.quantidade_porcoes           is 'Número de porções/unidades = rendimento (base) / peso_por_unidade (base). Nulo quando peso_por_unidade não informado.';
comment on column produtos.custo_por_grama_ml           is 'Custo total por grama ou mililitro.';
comment on column produtos.preco_por_grama_ml           is 'Preço sugerido por grama ou mililitro.';
comment on column produtos.custo_por_porcao             is 'Custo por porção/unidade individual.';
comment on column produtos.preco_por_porcao             is 'Preço sugerido por porção/unidade individual.';

-- =============================================================================
-- ÍNDICES
-- =============================================================================

-- Soft-delete
create index idx_usuarios_excluido_em         on usuarios(excluido_em)         where excluido_em is null;
create index idx_espacos_trabalho_excluido_em on espacos_trabalho(excluido_em) where excluido_em is null;
create index idx_ingredientes_excluido_em     on ingredientes(excluido_em)     where excluido_em is null;
create index idx_produtos_excluido_em         on produtos(excluido_em)         where excluido_em is null;
create index idx_pedidos_excluido_em          on pedidos(excluido_em)          where excluido_em is null;

-- Chaves estrangeiras / joins
create index idx_espacos_trabalho_proprietario   on espacos_trabalho(proprietario_id);
create index idx_membros_espaco_et               on membros_espaco_trabalho(espaco_trabalho_id);
create index idx_membros_usuario                 on membros_espaco_trabalho(usuario_id);
create index idx_produtos_espaco_trabalho        on produtos(espaco_trabalho_id);
create index idx_produtos_categoria              on produtos(categoria_id);
create index idx_ingredientes_espaco_trabalho    on ingredientes(espaco_trabalho_id);
create index idx_pedidos_espaco_trabalho         on pedidos(espaco_trabalho_id);
create index idx_pedidos_status                  on pedidos(status);
create index idx_pedidos_data_entrega            on pedidos(data_entrega);
create index idx_itens_pedido_pedido             on itens_pedido(pedido_id);
create index idx_ingredientes_produto_produto    on ingredientes_produto(produto_id);
create index idx_assinaturas_espaco_trabalho     on assinaturas(espaco_trabalho_id);

-- Tokens
create index idx_token_atualizacao_valor   on tokens_atualizacao(token);
create index idx_token_atualizacao_usuario on tokens_atualizacao(usuario_id);

-- Auditoria
create index idx_auditoria_entidade     on logs_auditoria(nome_entidade, id_entidade);
create index idx_auditoria_espaco       on logs_auditoria(espaco_trabalho_id);
create index idx_auditoria_realizado_em on logs_auditoria(realizado_em);

-- Receitas publicadas (consulta mais comum do dashboard)
create index idx_produtos_espaco_status
    on produtos(espaco_trabalho_id, status)
    where excluido_em is null and ativo = true;

-- Categorias de ingrediente (globais)
create unique index idx_categorias_ingrediente_nome
    on categorias_ingrediente(lower(nome))
    where excluido_em is null;

create index idx_categorias_ingrediente_ativo
    on categorias_ingrediente(id)
    where excluido_em is null;

-- Código único de ingrediente por espaço de trabalho
create unique index idx_ingredientes_espaco_codigo
    on ingredientes(espaco_trabalho_id, codigo)
    where codigo is not null and excluido_em is null;

create index idx_ingredientes_categoria
    on ingredientes(categoria_id)
    where excluido_em is null;

-- Histórico de preço
create index idx_historico_preco_ingrediente
    on historico_preco_ingrediente(ingrediente_id, alterado_em desc);

-- =============================================================================
-- SEED — Unidades de medida globais
-- =============================================================================

insert into unidades (nome, simbolo, tipo, base) values
  ('Grama',          'g',           'weight', true),
  ('Quilograma',     'kg',          'weight', false),
  ('Miligrama',      'mg',          'weight', false),
  ('Mililitro',      'ml',          'volume', true),
  ('Litro',          'L',           'volume', false),
  ('Colher de sopa', 'colher sopa', 'volume', false),
  ('Colher de chá',  'colher chá',  'volume', false),
  ('Xícara',         'xícara',      'volume', false),
  ('Unidade',        'un',          'unit',   true),
  ('Dúzia',          'dz',          'unit',   false),
  ('Pacote',         'pct',         'unit',   false);

-- Conversões de unidade (peso)
insert into conversoes_unidade (unidade_origem_id, unidade_destino_id, fator)
select f.id, t.id, c.fator
from (values
  ('kg',  'g',   1000.000000),
  ('g',   'kg',  0.001000),
  ('mg',  'g',   0.001000),
  ('g',   'mg',  1000.000000)
) as c(simbolo_origem, simbolo_destino, fator)
join unidades f on f.simbolo = c.simbolo_origem
join unidades t on t.simbolo = c.simbolo_destino;

-- Conversões de unidade (volume)
insert into conversoes_unidade (unidade_origem_id, unidade_destino_id, fator)
select f.id, t.id, c.fator
from (values
  ('L',           'ml', 1000.000000),
  ('ml',          'L',  0.001000),
  ('colher sopa', 'ml', 15.000000),
  ('colher chá',  'ml', 5.000000),
  ('xícara',      'ml', 240.000000)
) as c(simbolo_origem, simbolo_destino, fator)
join unidades f on f.simbolo = c.simbolo_origem
join unidades t on t.simbolo = c.simbolo_destino;

-- Conversões de unidade (contagem)
insert into conversoes_unidade (unidade_origem_id, unidade_destino_id, fator)
select f.id, t.id, c.fator
from (values
  ('dz', 'un', 12.000000),
  ('un', 'dz', 0.083333)
) as c(simbolo_origem, simbolo_destino, fator)
join unidades f on f.simbolo = c.simbolo_origem
join unidades t on t.simbolo = c.simbolo_destino;

-- =============================================================================
-- SEED — Categorias globais de produto
-- =============================================================================

insert into categorias_produto (nome, cor) values
  ('Bolo',           '#F59E0B'),
  ('Brigadeiro',     '#92400E'),
  ('Cupcake',        '#EC4899'),
  ('Torta',          '#D97706'),
  ('Cookie',         '#78350F'),
  ('Salgado',        '#10B981'),
  ('Mousse',         '#6366F1'),
  ('Doce de Festa',  '#EF4444'),
  ('Sobremesa',      '#8B5CF6'),
  ('Pão de Mel',     '#B45309'),
  ('Trufa',          '#1F2937'),
  ('Macarons',       '#F472B6'),
  ('Outros',         '#9CA3AF');

-- =============================================================================
-- SEED — Categorias globais de ingrediente
-- =============================================================================

insert into categorias_ingrediente (nome, cor) values
  ('Chocolate',              '#78350F'),
  ('Laticínio',              '#4FC3F7'),
  ('Farinha e Grãos',        '#D97706'),
  ('Açúcar e Adoçantes',     '#BA68C8'),
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

