-- Baseline acumulada del esquema hasta V4.
-- Flyway la aplica únicamente al crear una base vacía; no elimina datos ni esquemas.

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TYPE user_role AS ENUM ('OWNER', 'PLAYER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE venue_status AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');
CREATE TYPE football_format AS ENUM ('FIVE', 'SEVEN', 'ELEVEN');
CREATE TYPE surface_type AS ENUM ('SYNTHETIC_GRASS', 'NATURAL_GRASS', 'INDOOR', 'CONCRETE', 'OTHER');
CREATE TYPE reservation_status AS ENUM ('CONFIRMED', 'CANCELLED_BY_PLAYER', 'CANCELLED_BY_OWNER');
CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID');
CREATE TYPE court_occupancy_kind AS ENUM ('RESERVATION', 'BLOCK');

CREATE TABLE departments (
    code char(3) PRIMARY KEY,
    name varchar(60) NOT NULL UNIQUE,
    CONSTRAINT ck_departments_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT ck_departments_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email citext NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    national_id varchar(8) UNIQUE,
    phone varchar(20),
    role user_role NOT NULL,
    status user_status NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_email CHECK (btrim(email::text) <> '' AND length(email::text) <= 254),
    CONSTRAINT ck_users_password_hash CHECK (btrim(password_hash) <> ''),
    CONSTRAINT ck_users_first_name CHECK (btrim(first_name) <> ''),
    CONSTRAINT ck_users_last_name CHECK (btrim(last_name) <> ''),
    CONSTRAINT ck_users_national_id CHECK (national_id IS NULL OR national_id ~ '^[0-9]{7,8}$'),
    CONSTRAINT ck_users_phone CHECK (phone IS NULL OR phone ~ '^[+][1-9][0-9]{7,14}$')
);

CREATE TABLE venues (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name varchar(120) NOT NULL,
    description text,
    phone varchar(20),
    whatsapp_phone varchar(20),
    timezone varchar(64) NOT NULL DEFAULT 'America/Montevideo',
    status venue_status NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_venues_name CHECK (char_length(btrim(name)) BETWEEN 2 AND 120),
    CONSTRAINT ck_venues_phone CHECK (phone IS NULL OR phone ~ '^[+][1-9][0-9]{7,14}$'),
    CONSTRAINT ck_venues_whatsapp CHECK (whatsapp_phone IS NULL OR whatsapp_phone ~ '^[+][1-9][0-9]{7,14}$'),
    CONSTRAINT ck_venues_timezone CHECK (timezone = 'America/Montevideo')
);

CREATE TABLE venue_locations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id uuid NOT NULL UNIQUE REFERENCES venues(id) ON DELETE CASCADE,
    department_code char(3) NOT NULL REFERENCES departments(code) ON DELETE RESTRICT,
    city varchar(100) NOT NULL,
    neighborhood varchar(100),
    street varchar(120) NOT NULL,
    street_number varchar(20),
    reference text,
    latitude numeric(9,6) NOT NULL,
    longitude numeric(9,6) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_venue_locations_city CHECK (btrim(city) <> ''),
    CONSTRAINT ck_venue_locations_street CHECK (btrim(street) <> ''),
    CONSTRAINT ck_venue_locations_latitude CHECK (latitude BETWEEN -35.100000 AND -30.000000),
    CONSTRAINT ck_venue_locations_longitude CHECK (longitude BETWEEN -58.600000 AND -53.000000)
);

CREATE TABLE courts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id uuid NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    name varchar(80) NOT NULL,
    football_format football_format NOT NULL,
    surface surface_type NOT NULL,
    covered boolean NOT NULL DEFAULT false,
    price_per_slot numeric(12,2) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'UYU',
    slot_minutes smallint NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_courts_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_courts_price CHECK (price_per_slot >= 0),
    CONSTRAINT ck_courts_currency CHECK (currency = 'UYU'),
    CONSTRAINT ck_courts_slot_minutes CHECK (slot_minutes BETWEEN 30 AND 240 AND slot_minutes % 15 = 0)
);

CREATE TABLE venue_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id uuid NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    url text NOT NULL,
    storage_key text,
    sort_order smallint NOT NULL DEFAULT 0,
    is_cover boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_venue_images_url CHECK (btrim(url) <> ''),
    CONSTRAINT ck_venue_images_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE court_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    court_id uuid NOT NULL REFERENCES courts(id) ON DELETE CASCADE,
    url text NOT NULL,
    storage_key text,
    sort_order smallint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_court_images_url CHECK (btrim(url) <> ''),
    CONSTRAINT ck_court_images_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE opening_hours (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id uuid NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL,
    opens_at time NOT NULL,
    closes_at time NOT NULL,
    open_minute integer GENERATED ALWAYS AS (
        extract(hour FROM opens_at)::integer * 60 + extract(minute FROM opens_at)::integer
    ) STORED,
    close_minute integer GENERATED ALWAYS AS (
        extract(hour FROM closes_at)::integer * 60 + extract(minute FROM closes_at)::integer
    ) STORED,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_opening_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_opening_hours_order CHECK (closes_at > opens_at),
    CONSTRAINT ck_opening_hours_whole_minutes CHECK (
        extract(second FROM opens_at) = 0 AND extract(second FROM closes_at) = 0
    )
);

ALTER TABLE opening_hours
    ADD CONSTRAINT ex_opening_hours_no_overlap
    EXCLUDE USING gist (
        venue_id WITH =,
        day_of_week WITH =,
        int4range(open_minute, close_minute, '[)') WITH &&
    );

CREATE TABLE court_blocks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    court_id uuid NOT NULL REFERENCES courts(id) ON DELETE CASCADE,
    created_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    reason varchar(300) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_court_blocks_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_court_blocks_reason CHECK (char_length(btrim(reason)) BETWEEN 2 AND 300)
);

CREATE TABLE reservations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    court_id uuid NOT NULL REFERENCES courts(id) ON DELETE RESTRICT,
    player_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    status reservation_status NOT NULL DEFAULT 'CONFIRMED',
    price_amount numeric(12,2) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'UYU',
    player_name_snapshot varchar(161) NOT NULL,
    player_phone_snapshot varchar(20),
    notes varchar(500),
    payment_status payment_status NOT NULL DEFAULT 'PENDING',
    paid_at timestamptz,
    paid_confirmed_by_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reservations_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_reservations_price CHECK (price_amount >= 0),
    CONSTRAINT ck_reservations_currency CHECK (currency = 'UYU'),
    CONSTRAINT ck_reservations_player_name CHECK (btrim(player_name_snapshot) <> ''),
    CONSTRAINT ck_reservations_player_phone CHECK (
        player_phone_snapshot IS NULL OR player_phone_snapshot ~ '^[+][1-9][0-9]{7,14}$'
    ),
    CONSTRAINT ck_reservations_payment CHECK (
        (payment_status = 'PENDING' AND paid_at IS NULL AND paid_confirmed_by_user_id IS NULL)
        OR
        (payment_status = 'PAID' AND paid_at IS NOT NULL AND paid_confirmed_by_user_id IS NOT NULL)
    ),
    CONSTRAINT ck_reservations_cancellation CHECK (
        (status = 'CONFIRMED' AND cancelled_at IS NULL)
        OR
        (status IN ('CANCELLED_BY_PLAYER', 'CANCELLED_BY_OWNER') AND cancelled_at IS NOT NULL)
    ),
    CONSTRAINT ck_reservations_paid_not_cancelled CHECK (payment_status <> 'PAID' OR status = 'CONFIRMED')
);

-- Registro unificado para impedir cruces entre reservas y bloqueos.
CREATE TABLE court_occupancies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    court_id uuid NOT NULL REFERENCES courts(id) ON DELETE CASCADE,
    kind court_occupancy_kind NOT NULL,
    reservation_id uuid UNIQUE REFERENCES reservations(id) ON DELETE CASCADE,
    court_block_id uuid UNIQUE REFERENCES court_blocks(id) ON DELETE CASCADE,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    CONSTRAINT ck_court_occupancies_source CHECK (
        (kind = 'RESERVATION' AND reservation_id IS NOT NULL AND court_block_id IS NULL)
        OR
        (kind = 'BLOCK' AND reservation_id IS NULL AND court_block_id IS NOT NULL)
    ),
    CONSTRAINT ck_court_occupancies_range CHECK (ends_at > starts_at)
);

ALTER TABLE court_occupancies
    ADD CONSTRAINT ex_court_occupancies_no_overlap
    EXCLUDE USING gist (
        court_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    );

CREATE UNIQUE INDEX uq_venue_images_one_cover
    ON venue_images (venue_id) WHERE is_cover;
CREATE INDEX ix_venues_owner_name ON venues (owner_id, name);
CREATE INDEX ix_venues_status_name ON venues (status, name);
CREATE INDEX ix_venue_locations_department ON venue_locations (department_code);
CREATE INDEX ix_courts_venue_name ON courts (venue_id, name);
CREATE INDEX ix_venue_images_venue_sort ON venue_images (venue_id, sort_order);
CREATE INDEX ix_court_images_court_sort ON court_images (court_id, sort_order);
CREATE INDEX ix_opening_hours_venue_day_open ON opening_hours (venue_id, day_of_week, opens_at);
CREATE INDEX ix_court_blocks_court_starts ON court_blocks (court_id, starts_at);
CREATE INDEX ix_court_blocks_created_by ON court_blocks (created_by_user_id);
CREATE INDEX ix_reservations_player_starts ON reservations (player_id, starts_at DESC) WHERE player_id IS NOT NULL;
CREATE INDEX ix_reservations_court_starts ON reservations (court_id, starts_at);
CREATE INDEX ix_reservations_created_by ON reservations (created_by_user_id);
CREATE INDEX ix_reservations_paid_confirmed_by ON reservations (paid_confirmed_by_user_id)
    WHERE paid_confirmed_by_user_id IS NOT NULL;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_venues_updated_at BEFORE UPDATE ON venues
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_venue_locations_updated_at BEFORE UPDATE ON venue_locations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_courts_updated_at BEFORE UPDATE ON courts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_opening_hours_updated_at BEFORE UPDATE ON opening_hours
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_court_blocks_updated_at BEFORE UPDATE ON court_blocks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tr_reservations_updated_at BEFORE UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION validate_venue_owner()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    owner_role user_role;
BEGIN
    SELECT role INTO owner_role FROM users WHERE id = NEW.owner_id;
    IF owner_role IS NULL OR owner_role <> 'OWNER' THEN
        RAISE EXCEPTION 'El responsable del complejo debe tener rol OWNER';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_venues_validate_owner
    BEFORE INSERT OR UPDATE OF owner_id ON venues
    FOR EACH ROW EXECUTE FUNCTION validate_venue_owner();

CREATE OR REPLACE FUNCTION validate_court_block_owner()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    venue_owner_id uuid;
    actor_role user_role;
BEGIN
    SELECT role INTO actor_role
      FROM users
     WHERE id = NEW.created_by_user_id;

    IF actor_role = 'ADMIN' THEN
        RETURN NEW;
    END IF;

    SELECT v.owner_id
      INTO venue_owner_id
      FROM courts c
      JOIN venues v ON v.id = c.venue_id
     WHERE c.id = NEW.court_id;

    IF venue_owner_id IS NULL OR venue_owner_id <> NEW.created_by_user_id THEN
        RAISE EXCEPTION 'El bloqueo debe ser creado por el propietario de la cancha o un administrador';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_court_blocks_validate_owner
    BEFORE INSERT OR UPDATE OF court_id, created_by_user_id ON court_blocks
    FOR EACH ROW EXECUTE FUNCTION validate_court_block_owner();

CREATE OR REPLACE FUNCTION validate_reservation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    court_slot_minutes smallint;
    court_price numeric(12,2);
    court_currency char(3);
    court_active boolean;
    current_venue_status venue_status;
    venue_owner_id uuid;
    actor_role user_role;
    local_start timestamp;
    local_end timestamp;
    local_day smallint;
BEGIN
    SELECT c.slot_minutes, c.price_per_slot, c.currency, c.active, v.status, v.owner_id
      INTO court_slot_minutes, court_price, court_currency, court_active, current_venue_status, venue_owner_id
      FROM courts c
      JOIN venues v ON v.id = c.venue_id
     WHERE c.id = NEW.court_id;

    IF court_slot_minutes IS NULL THEN
        RAISE EXCEPTION 'La cancha de la reserva no existe';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF NOT court_active OR current_venue_status <> 'ACTIVE' THEN
            RAISE EXCEPTION 'La cancha no esta disponible';
        END IF;

        IF NEW.ends_at - NEW.starts_at <> make_interval(mins => court_slot_minutes) THEN
            RAISE EXCEPTION 'La reserva debe durar exactamente % minutos', court_slot_minutes;
        END IF;

        local_start := NEW.starts_at AT TIME ZONE 'America/Montevideo';
        local_end := NEW.ends_at AT TIME ZONE 'America/Montevideo';
        IF local_start::date <> local_end::date THEN
            RAISE EXCEPTION 'La reserva debe comenzar y terminar el mismo dia local';
        END IF;

        local_day := extract(isodow FROM local_start)::smallint;
        IF NOT EXISTS (
            SELECT 1
              FROM opening_hours oh
             WHERE oh.venue_id = (
                       SELECT venue_id FROM courts WHERE id = NEW.court_id
                   )
               AND oh.day_of_week = local_day
               AND local_start::time >= oh.opens_at
               AND local_end::time <= oh.closes_at
               AND mod(
                       extract(epoch FROM (local_start::time - oh.opens_at)),
                       (court_slot_minutes * 60)::numeric
                   ) = 0
        ) THEN
            RAISE EXCEPTION 'La reserva no coincide con un horario de apertura o con el inicio de un turno';
        END IF;

        IF NEW.price_amount IS DISTINCT FROM court_price OR NEW.currency IS DISTINCT FROM court_currency THEN
            RAISE EXCEPTION 'El precio y la moneda deben coincidir con los de la cancha';
        END IF;

        SELECT role INTO actor_role FROM users WHERE id = NEW.created_by_user_id;
        IF actor_role = 'PLAYER' THEN
            IF NEW.player_id IS DISTINCT FROM NEW.created_by_user_id THEN
                RAISE EXCEPTION 'Una reserva de jugador debe pertenecer al usuario que la crea';
            END IF;
        ELSIF actor_role = 'OWNER' THEN
            IF NEW.created_by_user_id <> venue_owner_id OR NEW.player_id IS NOT NULL THEN
                RAISE EXCEPTION 'El propietario no puede crear esta reserva manual';
            END IF;
        ELSIF actor_role = 'ADMIN' THEN
            IF NEW.player_id IS NOT NULL THEN
                RAISE EXCEPTION 'Una reserva manual no debe asociar un jugador registrado';
            END IF;
        ELSE
            RAISE EXCEPTION 'El usuario creador de la reserva no es valido';
        END IF;
    ELSE
        IF NEW.court_id IS DISTINCT FROM OLD.court_id
           OR NEW.player_id IS DISTINCT FROM OLD.player_id
           OR NEW.created_by_user_id IS DISTINCT FROM OLD.created_by_user_id
           OR NEW.starts_at IS DISTINCT FROM OLD.starts_at
           OR NEW.ends_at IS DISTINCT FROM OLD.ends_at
           OR NEW.price_amount IS DISTINCT FROM OLD.price_amount
           OR NEW.currency IS DISTINCT FROM OLD.currency THEN
            RAISE EXCEPTION 'No se pueden modificar los datos estructurales de una reserva';
        END IF;

        IF NEW.status IS DISTINCT FROM OLD.status AND NOT (
            OLD.status = 'CONFIRMED'
            AND NEW.status IN ('CANCELLED_BY_PLAYER', 'CANCELLED_BY_OWNER')
            AND OLD.payment_status = 'PENDING'
        ) THEN
            RAISE EXCEPTION 'Transicion de estado de reserva invalida';
        END IF;

        IF NEW.payment_status IS DISTINCT FROM OLD.payment_status AND NOT (
            OLD.payment_status = 'PENDING'
            AND NEW.payment_status = 'PAID'
            AND NEW.status = 'CONFIRMED'
        ) THEN
            RAISE EXCEPTION 'Transicion de pago invalida';
        END IF;
    END IF;

    IF NEW.payment_status = 'PAID' THEN
        IF NEW.paid_confirmed_by_user_id IS DISTINCT FROM venue_owner_id THEN
            RAISE EXCEPTION 'El pago debe ser confirmado por el propietario del complejo';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_reservations_validate
    BEFORE INSERT OR UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION validate_reservation();

CREATE OR REPLACE FUNCTION validate_court_occupancy_source()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    source_court_id uuid;
    source_starts_at timestamptz;
    source_ends_at timestamptz;
    source_status reservation_status;
BEGIN
    IF NEW.kind = 'RESERVATION' THEN
        SELECT court_id, starts_at, ends_at, status
          INTO source_court_id, source_starts_at, source_ends_at, source_status
          FROM reservations
         WHERE id = NEW.reservation_id;

        IF source_status IS DISTINCT FROM 'CONFIRMED' THEN
            RAISE EXCEPTION 'Solo una reserva confirmada puede ocupar una cancha';
        END IF;
    ELSE
        SELECT court_id, starts_at, ends_at
          INTO source_court_id, source_starts_at, source_ends_at
          FROM court_blocks
         WHERE id = NEW.court_block_id;
    END IF;

    IF source_court_id IS NULL
       OR NEW.court_id IS DISTINCT FROM source_court_id
       OR NEW.starts_at IS DISTINCT FROM source_starts_at
       OR NEW.ends_at IS DISTINCT FROM source_ends_at THEN
        RAISE EXCEPTION 'La ocupacion no coincide con su reserva o bloqueo de origen';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_court_occupancies_validate_source
    BEFORE INSERT OR UPDATE ON court_occupancies
    FOR EACH ROW EXECUTE FUNCTION validate_court_occupancy_source();

CREATE OR REPLACE FUNCTION sync_reservation_occupancy()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM court_occupancies
         WHERE reservation_id = OLD.id;
        RETURN OLD;
    END IF;

    IF NEW.status = 'CONFIRMED' THEN
        INSERT INTO court_occupancies (court_id, kind, reservation_id, starts_at, ends_at)
        VALUES (NEW.court_id, 'RESERVATION', NEW.id, NEW.starts_at, NEW.ends_at)
        ON CONFLICT (reservation_id) DO UPDATE
            SET court_id = EXCLUDED.court_id,
                starts_at = EXCLUDED.starts_at,
                ends_at = EXCLUDED.ends_at;
    ELSE
        DELETE FROM court_occupancies
         WHERE reservation_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_reservations_sync_occupancy
    AFTER INSERT OR UPDATE OR DELETE ON reservations
    FOR EACH ROW EXECUTE FUNCTION sync_reservation_occupancy();

CREATE OR REPLACE FUNCTION sync_court_block_occupancy()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM court_occupancies
         WHERE court_block_id = OLD.id;
        RETURN OLD;
    END IF;

    INSERT INTO court_occupancies (court_id, kind, court_block_id, starts_at, ends_at)
    VALUES (NEW.court_id, 'BLOCK', NEW.id, NEW.starts_at, NEW.ends_at)
    ON CONFLICT (court_block_id) DO UPDATE
        SET court_id = EXCLUDED.court_id,
            starts_at = EXCLUDED.starts_at,
            ends_at = EXCLUDED.ends_at;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_court_blocks_sync_occupancy
    AFTER INSERT OR UPDATE OR DELETE ON court_blocks
    FOR EACH ROW EXECUTE FUNCTION sync_court_block_occupancy();

INSERT INTO departments (code, name) VALUES
    ('ART', 'Artigas'),
    ('CAN', 'Canelones'),
    ('CLA', 'Cerro Largo'),
    ('COL', 'Colonia'),
    ('DUR', 'Durazno'),
    ('FLO', 'Flores'),
    ('FDA', 'Florida'),
    ('LAV', 'Lavalleja'),
    ('MAL', 'Maldonado'),
    ('MON', 'Montevideo'),
    ('PAY', 'Paysandú'),
    ('RNE', 'Río Negro'),
    ('RIV', 'Rivera'),
    ('ROC', 'Rocha'),
    ('SAL', 'Salto'),
    ('SJO', 'San José'),
    ('SOR', 'Soriano'),
    ('TAC', 'Tacuarembó'),
    ('TYT', 'Treinta y Tres');

