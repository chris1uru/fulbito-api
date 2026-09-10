-- Endurecimiento posterior a la auditoria: sesiones revocables, trazabilidad,
-- limites de texto y permisos de esquema. No elimina datos existentes.

ALTER TABLE users
    ADD COLUMN auth_version integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_users_auth_version CHECK (auth_version >= 0);

CREATE TABLE revoked_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_revoked_tokens_expiration CHECK (expires_at > revoked_at)
);
CREATE INDEX ix_revoked_tokens_expiration ON revoked_tokens (expires_at);

CREATE TABLE audit_events (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    http_method varchar(8) NOT NULL,
    request_path varchar(300) NOT NULL,
    response_status smallint NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_events_method CHECK (http_method IN ('POST','PUT','PATCH','DELETE')),
    CONSTRAINT ck_audit_events_status CHECK (response_status BETWEEN 100 AND 599)
);
CREATE INDEX ix_audit_events_actor_time ON audit_events (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_events_time ON audit_events (occurred_at DESC);

ALTER TABLE venues
    ADD CONSTRAINT ck_venues_description_length
        CHECK (description IS NULL OR char_length(description) <= 2000) NOT VALID;
ALTER TABLE venue_locations
    ADD CONSTRAINT ck_venue_locations_reference_length
        CHECK (reference IS NULL OR char_length(reference) <= 300) NOT VALID;

ALTER TABLE venues VALIDATE CONSTRAINT ck_venues_description_length;
ALTER TABLE venue_locations VALIDATE CONSTRAINT ck_venue_locations_reference_length;

-- Evita que cualquier rol nuevo pueda crear objetos en el esquema compartido.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
