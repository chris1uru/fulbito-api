ALTER TABLE users
    ADD COLUMN IF NOT EXISTS national_id varchar(8);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_national_id
    ON users (national_id)
    WHERE national_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_national_id'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_national_id
            CHECK (national_id IS NULL OR national_id ~ '^[0-9]{7,8}$');
    END IF;
END;
$$;

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
