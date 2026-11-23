-- ADP Automate / AIS Doc Manager baseline schema
-- Phase 1: accounts only. Domain tables land in later migrations.

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    username        VARCHAR(32)  NOT NULL UNIQUE,
    display_name    VARCHAR(200) NOT NULL,
    designation     VARCHAR(120),
    email           VARCHAR(320),
    role            VARCHAR(32)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change     BOOLEAN      NOT NULL DEFAULT FALSE,
    salt            VARCHAR(64)  NOT NULL,
    hash            VARCHAR(128) NOT NULL,
    iter            INTEGER      NOT NULL DEFAULT 210000,
    failed          INTEGER      NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(32),
    updated_at      TIMESTAMPTZ  NOT NULL,
    updated_by      VARCHAR(32),
    last_login_at   TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ
);

CREATE INDEX idx_accounts_active_name ON accounts (active, display_name);

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    actor_username  VARCHAR(32),
    actor_user_id   UUID,
    action          VARCHAR(80)  NOT NULL,
    entity_type     VARCHAR(80),
    entity_id       VARCHAR(80),
    detail          JSONB,
    ip_address      VARCHAR(64)
);

CREATE INDEX idx_audit_occurred ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
