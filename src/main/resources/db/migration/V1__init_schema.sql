-- ============================================================
-- V1 – Initial Schema
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── categories ───────────────────────────────────────────────
CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_slug      ON categories(slug);

-- ── users ────────────────────────────────────────────────────
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    phone        VARCHAR(30),
    company_name VARCHAR(200),
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('BUYER', 'SUPPLIER', 'ADMIN')),
    fcm_token    VARCHAR(500),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email   ON users(email);
CREATE INDEX idx_users_role    ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled);

-- ── user_categories (supplier coverage) ─────────────────────
CREATE TABLE user_categories (
    user_id     UUID   NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, category_id)
);

CREATE INDEX idx_user_categories_category_id ON user_categories(category_id);

-- ── demandes ─────────────────────────────────────────────────
CREATE TABLE demandes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    quantity       INTEGER,
    unit           VARCHAR(50),
    deadline       DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    category_id    BIGINT      NOT NULL REFERENCES categories(id),
    buyer_id       UUID        NOT NULL REFERENCES users(id),
    attachment_url VARCHAR(500),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_demandes_category_id ON demandes(category_id);
CREATE INDEX idx_demandes_buyer_id    ON demandes(buyer_id);
CREATE INDEX idx_demandes_status      ON demandes(status);
CREATE INDEX idx_demandes_created_at  ON demandes(created_at DESC);

-- ── reponses ─────────────────────────────────────────────────
CREATE TABLE reponses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_id  UUID        NOT NULL REFERENCES demandes(id) ON DELETE CASCADE,
    supplier_id UUID        NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('DISPONIBLE', 'INDISPONIBLE')),
    note        TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reponse_demande_supplier UNIQUE (demande_id, supplier_id)
);

CREATE INDEX idx_reponses_demande_id  ON reponses(demande_id);
CREATE INDEX idx_reponses_supplier_id ON reponses(supplier_id);
CREATE INDEX idx_reponses_status      ON reponses(status);
