CREATE SCHEMA IF NOT EXISTS samply;

SET
search_path TO samply;

CREATE TABLE samply.project
(
    id              SERIAL NOT NULL PRIMARY KEY,
    state_machine_key           TEXT,
    name          TEXT,
    contact_id      TEXT,
    state      TEXT,
    expiration_date DATE,
    created_at      TIMESTAMP,
    expires_at      TIMESTAMP,
    archived_at     TIMESTAMP
);
