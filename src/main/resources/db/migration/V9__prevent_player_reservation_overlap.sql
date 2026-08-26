-- Un jugador no puede ocupar dos canchas al mismo tiempo. Las reservas manuales
-- no tienen player_id y, por lo tanto, no quedan afectadas por esta regla.
-- El bloqueo asesor por jugador evita carreras entre dos inserciones concurrentes.
-- Se usa un trigger en vez de una exclusión porque ya existen reservas históricas
-- superpuestas y la migración no debe cancelar ni modificar esos datos.
CREATE OR REPLACE FUNCTION prevent_player_reservation_overlap()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 'CONFIRMED' AND NEW.player_id IS NOT NULL THEN
        PERFORM pg_advisory_xact_lock(hashtextextended(NEW.player_id::text, 0));

        IF EXISTS (
            SELECT 1
              FROM reservations r
             WHERE r.player_id = NEW.player_id
               AND r.status = 'CONFIRMED'
               AND r.id IS DISTINCT FROM NEW.id
               AND tstzrange(r.starts_at, r.ends_at, '[)')
                   && tstzrange(NEW.starts_at, NEW.ends_at, '[)')
        ) THEN
            RAISE EXCEPTION USING
                ERRCODE = '23505',
                CONSTRAINT = 'ex_reservations_player_no_overlap',
                MESSAGE = 'El jugador ya tiene una reserva confirmada en ese horario';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_reservations_prevent_player_overlap
    BEFORE INSERT OR UPDATE OF player_id, status, starts_at, ends_at ON reservations
    FOR EACH ROW EXECUTE FUNCTION prevent_player_reservation_overlap();
