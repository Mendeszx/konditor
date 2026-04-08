-- =============================================================================
-- KONDITOR — Migration: Receitas — status e preço sugerido
-- Aplique este script no banco de dados antes de implantar a nova versão do backend.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Coluna `status` na tabela products
--    Valores válidos: 'rascunho' | 'publicada'
--    DEFAULT 'publicada' garante retrocompatibilidade com receitas já existentes.
-- ---------------------------------------------------------------------------
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'publicada';

-- Garante que apenas valores esperados são aceitos
ALTER TABLE products
    ADD CONSTRAINT chk_products_status
        CHECK (status IN ('rascunho', 'publicada'));

-- ---------------------------------------------------------------------------
-- 2. Coluna `suggested_price` na tabela products
--    Preço sugerido calculado pelo servidor; pode ser nulo (receita sem ingredientes).
-- ---------------------------------------------------------------------------
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS suggested_price numeric;

-- ---------------------------------------------------------------------------
-- 3. Índices para performance
--    Filtra receitas publicadas no dashboard (query mais comum).
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_products_workspace_status
    ON products (workspace_id, status)
    WHERE deleted_at IS NULL AND is_active = TRUE;

-- =============================================================================
-- Verificação após migração:
--   SELECT status, count(*) FROM products GROUP BY status;
--   → Todos os registros anteriores devem aparecer como 'publicada'.
-- =============================================================================

