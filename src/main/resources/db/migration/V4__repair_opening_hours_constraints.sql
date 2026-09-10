-- La base inicial podía conservar horarios con segundos y una restricción de
-- exclusión anterior. Se normalizan los datos y se reconstruye la regla para
-- que solo se comparen franjas del mismo complejo y del mismo día.
UPDATE opening_hours
   SET opens_at = make_time(
           extract(hour FROM opens_at)::integer,
           extract(minute FROM opens_at)::integer,
           0
       ),
       closes_at = make_time(
           extract(hour FROM closes_at)::integer,
           extract(minute FROM closes_at)::integer,
           0
       )
 WHERE extract(second FROM opens_at) <> 0
    OR extract(second FROM closes_at) <> 0;

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT conname
          FROM pg_constraint
         WHERE conrelid = 'opening_hours'::regclass
           AND contype = 'x'
    LOOP
        EXECUTE format(
            'ALTER TABLE opening_hours DROP CONSTRAINT %I',
            constraint_name
        );
    END LOOP;
END;
$$;

ALTER TABLE opening_hours
    DROP CONSTRAINT IF EXISTS ck_opening_hours_whole_minutes;

ALTER TABLE opening_hours
    ADD CONSTRAINT ck_opening_hours_whole_minutes CHECK (
        extract(second FROM opens_at) = 0
        AND extract(second FROM closes_at) = 0
    );

ALTER TABLE opening_hours
    ADD CONSTRAINT ex_opening_hours_no_overlap
    EXCLUDE USING gist (
        venue_id WITH =,
        day_of_week WITH =,
        int4range(open_minute, close_minute, '[)') WITH &&
    );
