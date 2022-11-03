-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- clean data
TRUNCATE localmachine.event_payload_backup;
TRUNCATE localmachine.event_payload;
TRUNCATE localmachine.events;

-- drop group columns
ALTER TABLE localmachine.events
    DROP COLUMN IF EXISTS groupsequencenumber;

-- add new columns
ALTER TABLE localmachine.events
    ADD COLUMN parentid UUID NULL;
