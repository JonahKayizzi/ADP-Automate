CREATE TABLE processing_files (
    id                  UUID PRIMARY KEY,
    owner_type          VARCHAR(32)  NOT NULL,
    owner_id            UUID         NOT NULL,
    record_kind         VARCHAR(40)  NOT NULL,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX idx_processing_files_owner
    ON processing_files (owner_type, owner_id)
    WHERE retired = FALSE;

CREATE TABLE processing_forms (
    id                  UUID PRIMARY KEY,
    processing_file_id  UUID         NOT NULL REFERENCES processing_files (id),
    step_number         SMALLINT     NOT NULL,
    form_code           VARCHAR(40)  NOT NULL,
    form_title          VARCHAR(200) NOT NULL,
    controlled_notice   VARCHAR(500) NOT NULL,
    payload             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32),
    UNIQUE (processing_file_id, step_number)
);

CREATE INDEX idx_processing_forms_file ON processing_forms (processing_file_id);

CREATE TABLE processing_signatures (
    id                  UUID PRIMARY KEY,
    form_id             UUID         NOT NULL REFERENCES processing_forms (id) ON DELETE CASCADE,
    slot_key            VARCHAR(64)  NOT NULL,
    slot_label          VARCHAR(120) NOT NULL,
    signer_name         VARCHAR(200),
    designation         VARCHAR(200),
    signed_at           TIMESTAMPTZ,
    signed_by           UUID,
    signed_by_account   VARCHAR(32),
    UNIQUE (form_id, slot_key)
);

CREATE INDEX idx_processing_signatures_form ON processing_signatures (form_id);
