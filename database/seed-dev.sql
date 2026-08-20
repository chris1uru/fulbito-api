-- Datos de demostracion idempotentes para fulbito_dev.
-- No elimina ni modifica usuarios. No se ejecuta automaticamente con Flyway.
BEGIN;

DO $$
DECLARE
    demo_owner_id uuid;
    second_venue_id uuid;
    demo_venue_id constant uuid := 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d01';
    five_court_id constant uuid := 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d11';
    seven_court_id constant uuid := 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d12';
    seed_day smallint;
    local_tomorrow date := (CURRENT_TIMESTAMP AT TIME ZONE 'America/Montevideo')::date + 1;
    five_start timestamptz;
    seven_start timestamptz;
    block_start timestamptz;
BEGIN
    SELECT id
      INTO demo_owner_id
      FROM users
     WHERE role = 'OWNER' AND status = 'ACTIVE'
     ORDER BY created_at, id
     LIMIT 1;

    IF demo_owner_id IS NULL THEN
        RAISE NOTICE 'No se crean datos demo porque no existe un OWNER activo';
        RETURN;
    END IF;

    -- Completa el complejo existente que no tenia horarios, sin superponer
    -- configuraciones que el usuario haya agregado manualmente.
    SELECT id
      INTO second_venue_id
      FROM venues
     WHERE name = '2do Complejo'
     ORDER BY created_at, id
     LIMIT 1;

    IF second_venue_id IS NOT NULL THEN
        FOR seed_day IN 1..7 LOOP
            IF NOT EXISTS (
                SELECT 1
                  FROM opening_hours
                 WHERE venue_id = second_venue_id
                   AND day_of_week = seed_day
            ) THEN
                INSERT INTO opening_hours (venue_id, day_of_week, opens_at, closes_at)
                VALUES (
                    second_venue_id,
                    seed_day,
                    CASE WHEN seed_day <= 5 THEN time '16:00' ELSE time '10:00' END,
                    time '23:00'
                );
            END IF;
        END LOOP;
    END IF;

    INSERT INTO venues (
        id, owner_id, name, description, phone, whatsapp_phone,
        timezone, status, cancellation_notice_hours
    ) VALUES (
        demo_venue_id,
        demo_owner_id,
        'Arena Campus',
        'Complejo de demostracion con canchas de futbol 5 y 7.',
        '+59899123456',
        '+59899123456',
        'America/Montevideo',
        'ACTIVE',
        4
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO venue_locations (
        id, venue_id, department_code, city, neighborhood, street,
        street_number, reference, latitude, longitude
    ) VALUES (
        'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d02',
        demo_venue_id,
        'MAL',
        'Maldonado',
        'Centro',
        'Ituzaingo',
        '650',
        'Datos de demostracion',
        -34.904800,
        -54.958500
    )
    ON CONFLICT (venue_id) DO NOTHING;

    INSERT INTO courts (
        id, venue_id, name, football_format, surface, covered,
        price_per_slot, currency, slot_minutes, active
    ) VALUES
        (
            five_court_id, demo_venue_id, 'Campus 5', 'FIVE', 'INDOOR', true,
            1600, 'UYU', 60, true
        ),
        (
            seven_court_id, demo_venue_id, 'Campus 7', 'SEVEN',
            'SYNTHETIC_GRASS', false, 2400, 'UYU', 90, true
        )
    ON CONFLICT (id) DO NOTHING;

    FOR seed_day IN 1..7 LOOP
        IF NOT EXISTS (
            SELECT 1
              FROM opening_hours
             WHERE venue_id = demo_venue_id
               AND day_of_week = seed_day
        ) THEN
            INSERT INTO opening_hours (venue_id, day_of_week, opens_at, closes_at)
            VALUES (
                demo_venue_id,
                seed_day,
                CASE WHEN seed_day <= 5 THEN time '16:00' ELSE time '10:00' END,
                time '23:30'
            );
        END IF;
    END LOOP;

    -- Mañana a las 19:00 ambas canchas quedan ocupadas; a las 20:00
    -- Campus 5 vuelve a estar libre. Esto permite comprobar markers gris/verde.
    five_start := (local_tomorrow + time '19:00') AT TIME ZONE 'America/Montevideo';
    seven_start := five_start;
    block_start := (local_tomorrow + time '21:00') AT TIME ZONE 'America/Montevideo';

    IF NOT EXISTS (
        SELECT 1 FROM reservations
         WHERE id = 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d21'
    ) THEN
        INSERT INTO reservations (
            id, court_id, player_id, created_by_user_id, starts_at, ends_at,
            status, price_amount, currency, player_name_snapshot,
            player_phone_snapshot, notes, cancellation_notice_hours,
            late_cancellation, payment_status
        ) VALUES (
            'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d21',
            five_court_id,
            NULL,
            demo_owner_id,
            five_start,
            five_start + interval '60 minutes',
            'CONFIRMED',
            1600,
            'UYU',
            'Reserva demo WhatsApp',
            '+59899000001',
            'Dato de demostracion idempotente',
            4,
            false,
            'PENDING'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM reservations
         WHERE id = 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d22'
    ) THEN
        INSERT INTO reservations (
            id, court_id, player_id, created_by_user_id, starts_at, ends_at,
            status, price_amount, currency, player_name_snapshot,
            player_phone_snapshot, notes, cancellation_notice_hours,
            late_cancellation, payment_status
        ) VALUES (
            'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d22',
            seven_court_id,
            NULL,
            demo_owner_id,
            seven_start,
            seven_start + interval '90 minutes',
            'CONFIRMED',
            2400,
            'UYU',
            'Reserva demo telefonica',
            '+59899000002',
            'Dato de demostracion idempotente',
            4,
            false,
            'PENDING'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM court_blocks
         WHERE id = 'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d31'
    ) THEN
        INSERT INTO court_blocks (
            id, court_id, created_by_user_id, starts_at, ends_at, reason
        ) VALUES (
            'a0f6b911-0a7b-4a0a-8cf4-3c871ad71d31',
            five_court_id,
            demo_owner_id,
            block_start,
            block_start + interval '60 minutes',
            'Mantenimiento de demostracion'
        );
    END IF;
END
$$;

COMMIT;
