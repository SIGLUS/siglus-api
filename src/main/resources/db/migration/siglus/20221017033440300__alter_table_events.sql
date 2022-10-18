-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP INDEX IF EXISTS localmachine.localmachine_sender_localsequencenumber;

ALTER TABLE localmachine.events
    DROP COLUMN occurredtime;
ALTER TABLE localmachine.events
    ADD COLUMN occurredtime TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE localmachine.events
    ADD COLUMN syncedtime TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;



