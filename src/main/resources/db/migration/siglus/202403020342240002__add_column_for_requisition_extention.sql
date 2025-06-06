-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='requisition_extension' AND column_name='createdbyfacilityid'
    ) THEN
ALTER TABLE siglusintegration.requisition_extension
    ADD COLUMN createdbyfacilityid uuid;
END IF;
END $$;
