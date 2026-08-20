-- Cada complejo define su plazo de cancelación. El valor se copia a la reserva
-- para que un cambio posterior no altere las condiciones ya aceptadas.
ALTER TABLE venues
    ADD COLUMN cancellation_notice_hours smallint NOT NULL DEFAULT 4,
    ADD CONSTRAINT ck_venues_cancellation_notice_hours
        CHECK (cancellation_notice_hours BETWEEN 0 AND 168);

ALTER TABLE reservations
    ADD COLUMN cancellation_notice_hours smallint NOT NULL DEFAULT 4,
    ADD COLUMN late_cancellation boolean NOT NULL DEFAULT false;

UPDATE reservations r
   SET cancellation_notice_hours = v.cancellation_notice_hours
  FROM courts c
  JOIN venues v ON v.id = c.venue_id
 WHERE c.id = r.court_id;

ALTER TABLE reservations
    ALTER COLUMN cancellation_notice_hours DROP DEFAULT,
    ADD CONSTRAINT ck_reservations_cancellation_notice_hours
        CHECK (cancellation_notice_hours BETWEEN 0 AND 168),
    ADD CONSTRAINT ck_reservations_late_cancellation
        CHECK (NOT late_cancellation OR status = 'CANCELLED_BY_PLAYER');

CREATE OR REPLACE FUNCTION validate_reservation_cancellation_policy()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    venue_notice_hours smallint;
    expected_late boolean;
BEGIN
    IF TG_OP = 'INSERT' THEN
        SELECT v.cancellation_notice_hours
          INTO venue_notice_hours
          FROM courts c
          JOIN venues v ON v.id = c.venue_id
         WHERE c.id = NEW.court_id;

        IF venue_notice_hours IS NULL
           OR NEW.cancellation_notice_hours IS DISTINCT FROM venue_notice_hours THEN
            RAISE EXCEPTION 'La política de cancelación debe coincidir con la del complejo';
        END IF;

        IF NEW.late_cancellation THEN
            RAISE EXCEPTION 'Una reserva nueva no puede ser una cancelación tardía';
        END IF;
    ELSE
        IF NEW.cancellation_notice_hours IS DISTINCT FROM OLD.cancellation_notice_hours THEN
            RAISE EXCEPTION 'No se puede modificar la política de una reserva existente';
        END IF;

        IF NEW.late_cancellation IS DISTINCT FROM OLD.late_cancellation
           AND NOT (
               OLD.status = 'CONFIRMED'
               AND NEW.status = 'CANCELLED_BY_PLAYER'
           ) THEN
            RAISE EXCEPTION 'No se puede modificar la marca de cancelación tardía';
        END IF;

        IF OLD.status = 'CONFIRMED'
           AND NEW.status = 'CANCELLED_BY_PLAYER' THEN
            expected_late := NEW.cancelled_at > (
                NEW.starts_at - make_interval(hours => NEW.cancellation_notice_hours)
            );

            IF NEW.late_cancellation IS DISTINCT FROM expected_late THEN
                RAISE EXCEPTION 'La marca de cancelación tardía no coincide con el plazo de la reserva';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_reservations_validate_cancellation_policy
    BEFORE INSERT OR UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION validate_reservation_cancellation_policy();
