-- El endpoint ya autorizaba OWNER y ADMIN, pero la validación de base
-- sólo admitía al dueño. Se alinea la última barrera con la autorización.
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
        SELECT role
          INTO actor_role
          FROM users
         WHERE id = NEW.paid_confirmed_by_user_id;

        IF NEW.paid_confirmed_by_user_id IS DISTINCT FROM venue_owner_id
           AND actor_role IS DISTINCT FROM 'ADMIN' THEN
            RAISE EXCEPTION 'El pago debe ser confirmado por el propietario del complejo o un administrador';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

