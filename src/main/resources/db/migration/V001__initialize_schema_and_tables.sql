CREATE SCHEMA IF NOT EXISTS samply;

SET
search_path TO samply;

CREATE TABLE samply.query
(
    id            SERIAL PRIMARY KEY,
    query         TEXT NOT NULL,
    query_format  TEXT NOT NULL,
    output_format TEXT,
    template_id   TEXT,
    label         TEXT,
    description   TEXT,
    query_process TEXT
);

CREATE TABLE samply.project
(
    id                SERIAL NOT NULL PRIMARY KEY,
    state_machine_key TEXT,
    name              TEXT,
    state             TEXT,
    creator_email     TEXT,
    expiration_date   DATE,
    created_at        TIMESTAMP,
    expires_at        TIMESTAMP,
    archived_at       TIMESTAMP,
    query_id          BIGINT
);

CREATE TABLE samply.project_bridgehead
(
    id         SERIAL NOT NULL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    bridgehead TEXT   NOT NULL,
    state      TEXT   NOT NULL
);

CREATE TABLE samply.project_bridgehead_user
(
    id                    SERIAL NOT NULL PRIMARY KEY,
    project_bridgehead_id BIGINT,
    email                 TEXT,
    project_role          TEXT
);

CREATE TABLE samply.bridgehead_admin_user
(
    id         SERIAL NOT NULL PRIMARY KEY,
    email      TEXT,
    bridgehead TEXT
);

CREATE TABLE samply.project_manager_admin_user
(
    id    SERIAL NOT NULL PRIMARY KEY,
    email TEXT
);

CREATE TABLE samply.project_document
(
    id         SERIAL NOT NULL PRIMARY KEY,
    file_path  TEXT,
    url        TEXT,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP
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

CREATE INDEX idx_project_bridgehead_project_id ON samply.project_bridgehead (project_id);
CREATE INDEX idx_project_bridgehead_user_project_bridgehead_id ON samply.project_bridgehead_user (project_bridgehead_id);
CREATE INDEX idx_project_document_project_id ON samply.project_document (project_id);
CREATE INDEX idx_project_query_id ON samply.project (query_id);
