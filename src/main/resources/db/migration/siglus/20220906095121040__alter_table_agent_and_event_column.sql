-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE localmachine.events
    ALTER COLUMN payload TYPE bytea USING payload::TEXT::bytea;
ALTER TABLE localmachine.agents
    ALTER COLUMN publickey TYPE bytea USING publickey::TEXT::bytea;
ALTER TABLE localmachine.agents
    ALTER COLUMN privatekey TYPE bytea USING privatekey::TEXT::bytea;