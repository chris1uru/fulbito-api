CREATE TYPE match_style AS ENUM ('RECREATIONAL', 'COMPETITIVE');
CREATE TYPE match_request_status AS ENUM ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED');

CREATE TABLE match_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reservation_id uuid REFERENCES reservations(id) ON DELETE SET NULL,
    football_format football_format NOT NULL,
    style match_style NOT NULL,
    notes varchar(500),
    status match_request_status NOT NULL DEFAULT 'OPEN',
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_match_requests_notes CHECK (
        notes IS NULL OR char_length(btrim(notes)) BETWEEN 1 AND 500
    ),
    CONSTRAINT ck_match_requests_expiration CHECK (expires_at > created_at)
);

CREATE TABLE match_request_availabilities (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_request_id uuid NOT NULL REFERENCES match_requests(id) ON DELETE CASCADE,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_match_availability_range CHECK (ends_at > starts_at)
);

CREATE TABLE match_interests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_request_id uuid NOT NULL REFERENCES match_requests(id) ON DELETE CASCADE,
    player_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    player_name_snapshot varchar(161) NOT NULL,
    player_phone_snapshot varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_interest_player UNIQUE (match_request_id, player_id),
    CONSTRAINT ck_match_interest_name CHECK (btrim(player_name_snapshot) <> ''),
    CONSTRAINT ck_match_interest_phone CHECK (
        player_phone_snapshot ~ '^[+][1-9][0-9]{7,14}$'
    )
);

CREATE INDEX ix_match_requests_discovery
    ON match_requests (status, style, football_format, expires_at, created_at DESC);
CREATE INDEX ix_match_requests_creator
    ON match_requests (creator_user_id, created_at DESC);
CREATE INDEX ix_match_availabilities_start
    ON match_request_availabilities (starts_at, ends_at);
CREATE INDEX ix_match_interests_request
    ON match_interests (match_request_id, created_at);
CREATE UNIQUE INDEX uq_match_requests_open_reservation
    ON match_requests (reservation_id)
    WHERE reservation_id IS NOT NULL AND status = 'OPEN';

CREATE OR REPLACE FUNCTION validate_match_request_reservation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    reservation_player_id uuid;
    reservation_status_value reservation_status;
    reservation_starts_at timestamptz;
    reservation_format football_format;
BEGIN
    IF NEW.reservation_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT r.player_id, r.status, r.starts_at, c.football_format
      INTO reservation_player_id, reservation_status_value, reservation_starts_at, reservation_format
      FROM reservations r
      JOIN courts c ON c.id = r.court_id
     WHERE r.id = NEW.reservation_id;

    IF reservation_player_id IS NULL THEN
        RAISE EXCEPTION 'La reserva seleccionada no existe o no pertenece a un jugador';
    END IF;
    IF reservation_player_id IS DISTINCT FROM NEW.creator_user_id THEN
        RAISE EXCEPTION 'La reserva seleccionada no pertenece al creador de la busqueda';
    END IF;
    IF reservation_status_value IS DISTINCT FROM 'CONFIRMED' OR reservation_starts_at <= CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'La reserva seleccionada debe estar confirmada y ser futura';
    END IF;
    IF reservation_format IS DISTINCT FROM NEW.football_format THEN
        RAISE EXCEPTION 'El formato debe coincidir con la cancha reservada';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_match_requests_validate_reservation
    BEFORE INSERT OR UPDATE OF reservation_id, creator_user_id, football_format
    ON match_requests
    FOR EACH ROW EXECUTE FUNCTION validate_match_request_reservation();

CREATE OR REPLACE FUNCTION validate_match_interest()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    request_creator_id uuid;
    request_status match_request_status;
    request_expires_at timestamptz;
BEGIN
    SELECT creator_user_id, status, expires_at
      INTO request_creator_id, request_status, request_expires_at
      FROM match_requests
     WHERE id = NEW.match_request_id
     FOR UPDATE;

    IF request_creator_id IS NULL THEN
        RAISE EXCEPTION 'La busqueda seleccionada no existe';
    END IF;
    IF request_creator_id = NEW.player_id THEN
        RAISE EXCEPTION 'El creador no puede postularse a su propia busqueda';
    END IF;
    IF request_status IS DISTINCT FROM 'OPEN' OR request_expires_at <= CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'La busqueda ya no esta disponible';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_match_interests_validate
    BEFORE INSERT OR UPDATE ON match_interests
    FOR EACH ROW EXECUTE FUNCTION validate_match_interest();

CREATE OR REPLACE FUNCTION cancel_match_request_for_reservation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'CONFIRMED' AND NEW.status <> 'CONFIRMED' THEN
        UPDATE match_requests
           SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
         WHERE reservation_id = NEW.id AND status = 'OPEN';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_reservations_cancel_match_request
    AFTER UPDATE OF status ON reservations
    FOR EACH ROW EXECUTE FUNCTION cancel_match_request_for_reservation();
