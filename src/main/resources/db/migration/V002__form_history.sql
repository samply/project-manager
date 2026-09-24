-- History of form definitions: one row per form title and version, only ever
-- inserted. At startup, the configured forms are compared with their latest
-- version, and incompatible changes stop the project manager
-- (see plans/2026-09-24-plan-form-definition-snapshots.md).
CREATE TABLE samply.form_history
(
    id         SERIAL PRIMARY KEY,
    form_title TEXT      NOT NULL,
    version    INT       NOT NULL,
    checksum   TEXT      NOT NULL,
    definition TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE samply.form_history
    ADD CONSTRAINT uq_form_history_form_title_version
        UNIQUE (form_title, version);
