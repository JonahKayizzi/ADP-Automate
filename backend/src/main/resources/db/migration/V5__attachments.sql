CREATE TABLE attachments (
    id                  UUID PRIMARY KEY,
    owner_type          VARCHAR(32)  NOT NULL,
    owner_id            UUID         NOT NULL,
    original_filename   VARCHAR(500) NOT NULL,
    content_type        VARCHAR(120) NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    storage_key         VARCHAR(200) NOT NULL,
    sha256              CHAR(64)     NOT NULL,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE UNIQUE INDEX idx_attachments_storage_key ON attachments (storage_key);
CREATE INDEX idx_attachments_owner ON attachments (owner_type, owner_id) WHERE retired = FALSE;
CREATE INDEX idx_attachments_retired ON attachments (retired);
