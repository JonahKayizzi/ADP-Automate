CREATE TABLE complaints (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(500) NOT NULL,
    complainant         VARCHAR(300) NOT NULL,
    received            DATE         NOT NULL,
    channel             VARCHAR(40)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    description         TEXT,
    action_taken        TEXT,
    example             BOOLEAN      NOT NULL DEFAULT FALSE,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE INDEX idx_complaints_status_received ON complaints (status, received DESC) WHERE retired = FALSE;

CREATE TABLE tasks (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(500) NOT NULL,
    due_date            DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    priority            BOOLEAN      NOT NULL DEFAULT FALSE,
    assignee_username   VARCHAR(32),
    assignee_name       VARCHAR(200),
    assignee_designation VARCHAR(200),
    assignee_email      VARCHAR(320),
    assigned_at         TIMESTAMPTZ,
    assigned_by         VARCHAR(32),
    assigned_by_name    VARCHAR(200),
    seen_at             TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    notify_status       VARCHAR(20)  NOT NULL DEFAULT 'pending',
    notify_to           VARCHAR(32),
    notify_at           TIMESTAMPTZ,
    notify_by           VARCHAR(32),
    example             BOOLEAN      NOT NULL DEFAULT FALSE,
    retired             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          UUID,
    created_by_account  VARCHAR(32),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by_account  VARCHAR(32)
);

CREATE INDEX idx_tasks_status_due ON tasks (status, due_date) WHERE retired = FALSE;
CREATE INDEX idx_tasks_assignee ON tasks (assignee_username) WHERE retired = FALSE;
