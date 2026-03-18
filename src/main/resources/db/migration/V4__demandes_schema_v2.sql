-- V4: Add EXPIRED status support, closedAt/expiredAt columns, and additional indexes
-- Note: Flyway is disabled; schema managed by Hibernate ddl-auto=update

-- Extend demande status check to include EXPIRED
ALTER TABLE demandes DROP CONSTRAINT IF EXISTS demandes_status_check;
ALTER TABLE demandes ADD CONSTRAINT demandes_status_check
    CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED'));

ALTER TABLE demandes ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP;
ALTER TABLE demandes ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP;
ALTER TABLE demandes ADD COLUMN IF NOT EXISTS quality_score INTEGER NOT NULL DEFAULT 0;
ALTER TABLE demandes ADD COLUMN IF NOT EXISTS wilaya VARCHAR(100);

-- Add updated_at to reponses
ALTER TABLE reponses ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add composite scoring table if not exists
CREATE TABLE IF NOT EXISTS demande_supplier_scores (
    demande_id UUID NOT NULL REFERENCES demandes(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES users(id),
    category_score INTEGER NOT NULL DEFAULT 0,
    proximity_score INTEGER NOT NULL DEFAULT 0,
    urgency_score INTEGER NOT NULL DEFAULT 0,
    buyer_score INTEGER NOT NULL DEFAULT 0,
    quantity_score INTEGER NOT NULL DEFAULT 0,
    base_score INTEGER NOT NULL DEFAULT 0,
    decay_factor DECIMAL(4,2) NOT NULL DEFAULT 1.0,
    final_score INTEGER NOT NULL DEFAULT 0,
    scored_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_decay_at TIMESTAMP,
    PRIMARY KEY (demande_id, supplier_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_demandes_deadline ON demandes(deadline);
CREATE INDEX IF NOT EXISTS idx_scores_supplier_final ON demande_supplier_scores(supplier_id, final_score DESC);
CREATE INDEX IF NOT EXISTS idx_scores_demande ON demande_supplier_scores(demande_id);
