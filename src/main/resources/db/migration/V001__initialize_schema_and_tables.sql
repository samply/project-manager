CREATE SCHEMA IF NOT EXISTS samply;

SET
search_path TO samply;

CREATE TABLE samply.project
(
    id                SERIAL NOT NULL PRIMARY KEY,
    state_machine_key TEXT,
    name              TEXT,
    state             TEXT,
    expiration_date   DATE,
    created_at        TIMESTAMP,
    expires_at        TIMESTAMP,
    archived_at       TIMESTAMP
);

CREATE TABLE samply.user
(
    id                SERIAL NOT NULL PRIMARY KEY,
    email             TEXT,
    bridgehead        TEXT,
    organisation_role TEXT,
    created_at        TIMESTAMP,
    disabled_at       TIMESTAMP
);

CREATE TABLE samply.project_user
(
    id           SERIAL PRIMARY KEY,
    project_id   BIGINT,
    user_id      BIGINT,
    project_role TEXT
);

CREATE TABLE samply.project_bridgehead
(
    id         SERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    bridgehead TEXT   NOT NULL,
    state      TEXT   NOT NULL
);

ALTER TABLE samply.project_user
    ADD CONSTRAINT fk_project_id
        FOREIGN KEY (project_id)
            REFERENCES samply.project (id);

ALTER TABLE samply.project_user
    ADD CONSTRAINT fk_user_id
        FOREIGN KEY (user_id)
            REFERENCES samply.user (id);

ALTER TABLE samply.project_bridgehead
    ADD CONSTRAINT fk_project_id
        FOREIGN KEY (project_id)
            REFERENCES samply.project (id);

CREATE INDEX idx_project_user_project_id ON samply.project_user (project_id);
CREATE INDEX idx_project_user_user_id ON samply.project_user (user_id);
CREATE INDEX idx_project_bridgehead_project_id ON samply.project_bridgehead (project_id);
