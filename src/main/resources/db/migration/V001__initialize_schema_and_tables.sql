CREATE SCHEMA IF NOT EXISTS samply;

SET
    search_path TO samply;

CREATE TABLE samply.query
(
    id             SERIAL PRIMARY KEY,
    code           TEXT      NOT NULL,
    query          TEXT      NOT NULL,
    query_format   TEXT      NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    human_readable TEXT,
    explorer_url   TEXT,
    output_format  TEXT,
    template_id    TEXT,
    label          TEXT,
    description    TEXT,
    context        TEXT
);

CREATE TABLE samply.project
(
    id                SERIAL    NOT NULL PRIMARY KEY,
    state_machine_key TEXT      NOT NULL,
    code              TEXT      NOT NULL,
    state             TEXT      NOT NULL,
    creator_email     TEXT      NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    expires_at        DATE,
    archived_at       TIMESTAMP,
    modified_at       TIMESTAMP NOT NULL,
    query_id          BIGINT,
    type              TEXT
);

CREATE TABLE samply.project_bridgehead
(
    id          SERIAL    NOT NULL PRIMARY KEY,
    project_id  BIGINT    NOT NULL,
    bridgehead  TEXT      NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    state       TEXT      NOT NULL
);

CREATE TABLE samply.project_bridgehead_user
(
    id                    SERIAL    NOT NULL PRIMARY KEY,
    project_bridgehead_id BIGINT    NOT NULL,
    email                 TEXT      NOT NULL,
    project_role          TEXT      NOT NULL,
    project_state         TEXT      NOT NULL,
    modified_at           TIMESTAMP NOT NULL
);

CREATE TABLE samply.bridgehead_admin_user
(
    id         SERIAL NOT NULL PRIMARY KEY,
    email      TEXT   NOT NULL,
    bridgehead TEXT   NOT NULL
);

CREATE TABLE samply.project_manager_admin_user
(
    id    SERIAL NOT NULL PRIMARY KEY,
    email TEXT   NOT NULL
);

CREATE TABLE samply.project_document
(
    id                SERIAL    NOT NULL PRIMARY KEY,
    file_path         TEXT,
    original_filename TEXT,
    url               TEXT,
    project_id        BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    bridgehead        TEXT,
    type              TEXT      NOT NULL,
    creator_email     TEXT      NOT NULL,
    label             TEXT
);

CREATE TABLE samply.notification
(
    id             SERIAL                   NOT NULL PRIMARY KEY,
    email          TEXT,
    timestamp      TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id     BIGINT                   NOT NULL,
    bridgehead     TEXT,
    operation_type TEXT,
    details        TEXT,
    error          TEXT,
    http_status    TEXT
);

CREATE TABLE samply.notification_user_action
(
    id              SERIAL PRIMARY KEY,
    email           TEXT      NOT NULL,
    read            BOOLEAN   NOT NULL DEFAULT false,
    notification_id BIGINT    NOT NULL,
    modified_at     TIMESTAMP NOT NULL
);


ALTER TABLE samply.project
    ADD CONSTRAINT fk_project_query
        FOREIGN KEY (query_id)
            REFERENCES samply.query (id);

ALTER TABLE samply.project_bridgehead
    ADD CONSTRAINT fk_project_id
        FOREIGN KEY (project_id)
            REFERENCES samply.project (id);

ALTER TABLE samply.project_bridgehead_user
    ADD CONSTRAINT fk_project_bridgehead_id
        FOREIGN KEY (project_bridgehead_id)
            REFERENCES samply.project_bridgehead (id);

ALTER TABLE samply.project_document
    ADD CONSTRAINT fk_project_document_project
        FOREIGN KEY (project_id)
            REFERENCES samply.project (id);

ALTER TABLE samply.notification
    ADD CONSTRAINT fk_project_id
        FOREIGN KEY (project_id) REFERENCES samply.project (id);

ALTER TABLE samply.notification_user_action
    ADD CONSTRAINT fk_notification_id
        FOREIGN KEY (notification_id) REFERENCES samply.notification (id);

CREATE INDEX idx_project_bridgehead_project_id ON samply.project_bridgehead (project_id);
CREATE INDEX idx_project_bridgehead_user_project_bridgehead_id ON samply.project_bridgehead_user (project_bridgehead_id);
CREATE INDEX idx_project_document_project_id ON samply.project_document (project_id);
CREATE INDEX idx_project_query_id ON samply.project (query_id);
CREATE INDEX idx_notification_project_id ON samply.notification (project_id);
CREATE INDEX idx_notification_id ON samply.notification_user_action (notification_id);
