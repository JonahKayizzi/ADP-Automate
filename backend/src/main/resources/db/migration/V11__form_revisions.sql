CREATE TABLE processing_form_revisions (
    id                  UUID PRIMARY KEY,
    form_id             UUID         NOT NULL REFERENCES processing_forms (id) ON DELETE CASCADE,
    revision_number     INTEGER      NOT NULL,
    form_code           VARCHAR(40)  NOT NULL,
    payload             JSONB        NOT NULL,
    signatures          JSONB        NOT NULL,
    revised_at          TIMESTAMPTZ  NOT NULL,
    revised_by_account  VARCHAR(32)  NOT NULL,
    UNIQUE (form_id, revision_number)
);

CREATE INDEX idx_processing_form_revisions_form
    ON processing_form_revisions (form_id, revision_number DESC);
