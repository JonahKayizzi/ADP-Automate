CREATE TABLE notams (
    id                  UUID PRIMARY KEY,
    number              VARCHAR(16)  NOT NULL,
    issued              DATE         NOT NULL,
    title               VARCHAR(500) NOT NULL,
    originator          VARCHAR(80)  NOT NULL,
    valid_to            VARCHAR(32),
    example             BOOLEAN      NOT NULL DEFAULT FALSE,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE UNIQUE INDEX uq_notams_number_active ON notams (number) WHERE retired = FALSE;
CREATE INDEX idx_notams_issued ON notams (issued DESC);
CREATE INDEX idx_notams_retired ON notams (retired);
