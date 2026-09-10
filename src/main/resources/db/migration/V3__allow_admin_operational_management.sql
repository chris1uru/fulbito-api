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
