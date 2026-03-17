-- ============================================================
-- V3 – Category schema updates
-- Adds columns that Hibernate manages via ddl-auto in dev.
-- Applied in production when Flyway is re-enabled.
-- ============================================================

-- Extend name column to 200 chars (was 100)
ALTER TABLE categories ALTER COLUMN name TYPE VARCHAR(200);

-- Materialized path (dot-separated ancestor IDs: "1.11.111")
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS depth INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS node_type VARCHAR(20) NOT NULL DEFAULT 'SEEDED',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS demande_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

-- Backfill path for existing root nodes (depth = 0, parent_id IS NULL)
UPDATE categories SET path = id::TEXT WHERE parent_id IS NULL AND path IS NULL;

-- Backfill child paths (depth = 1) — 2-segment paths
UPDATE categories c
SET    path  = p.path || '.' || c.id::TEXT,
       depth = 1
FROM   categories p
WHERE  c.parent_id = p.id
  AND  c.path IS NULL;

-- For deeper levels you would run additional passes, but V2 only seeds depth-0 and depth-1 nodes.
-- DataInitializer handles path computation for deeper nodes at runtime.

-- Make path NOT NULL after backfill
ALTER TABLE categories ALTER COLUMN path SET NOT NULL;

-- Indexes for path-based queries
CREATE INDEX IF NOT EXISTS idx_category_path         ON categories(path);
CREATE INDEX IF NOT EXISTS idx_category_path_pattern ON categories(path text_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_category_active       ON categories(active);
CREATE INDEX IF NOT EXISTS idx_category_node_type    ON categories(node_type);

-- ── category_attributes ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS category_attributes (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT      NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    key          VARCHAR(100) NOT NULL,
    label        VARCHAR(200) NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    required     BOOLEAN      NOT NULL DEFAULT FALSE,
    inherited    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT         NOT NULL DEFAULT 0,
    options      TEXT
);

CREATE INDEX IF NOT EXISTS idx_category_attributes_category_id
    ON category_attributes(category_id);
