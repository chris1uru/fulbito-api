CREATE UNIQUE INDEX IF NOT EXISTS uq_venue_images_storage_key
    ON venue_images (storage_key)
    WHERE storage_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_court_images_storage_key
    ON court_images (storage_key)
    WHERE storage_key IS NOT NULL;

CREATE OR REPLACE FUNCTION enforce_venue_image_limit()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('venue-images:' || NEW.venue_id::text));
    IF (SELECT count(*) FROM venue_images WHERE venue_id = NEW.venue_id) >= 8 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_venue_images_max_8',
            MESSAGE = 'A venue can have at most 8 images';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION enforce_court_image_limit()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('court-images:' || NEW.court_id::text));
    IF (SELECT count(*) FROM court_images WHERE court_id = NEW.court_id) >= 5 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_court_images_max_5',
            MESSAGE = 'A court can have at most 5 images';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_venue_images_limit ON venue_images;
CREATE TRIGGER trg_venue_images_limit
BEFORE INSERT ON venue_images
FOR EACH ROW EXECUTE FUNCTION enforce_venue_image_limit();

DROP TRIGGER IF EXISTS trg_court_images_limit ON court_images;
CREATE TRIGGER trg_court_images_limit
BEFORE INSERT ON court_images
FOR EACH ROW EXECUTE FUNCTION enforce_court_image_limit();
