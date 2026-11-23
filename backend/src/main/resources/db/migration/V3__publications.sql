CREATE TABLE publications (
    id                  UUID PRIMARY KEY,
    type                VARCHAR(40)  NOT NULL,
    number              VARCHAR(32)  NOT NULL,
    title               VARCHAR(500) NOT NULL,
    effective           DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    example             BOOLEAN      NOT NULL DEFAULT FALSE,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE INDEX idx_publications_effective ON publications (effective DESC);
CREATE INDEX idx_publications_status ON publications (status);
CREATE INDEX idx_publications_retired ON publications (retired);
CREATE INDEX idx_publications_type ON publications (type);
