CREATE TABLE library_documents (
    id                  UUID PRIMARY KEY,
    doc_type            VARCHAR(40)  NOT NULL,
    title               VARCHAR(500) NOT NULL,
    edition             VARCHAR(200),
    example             BOOLEAN      NOT NULL DEFAULT FALSE,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE INDEX idx_library_type_title ON library_documents (doc_type, title);
CREATE INDEX idx_library_retired ON library_documents (retired);
