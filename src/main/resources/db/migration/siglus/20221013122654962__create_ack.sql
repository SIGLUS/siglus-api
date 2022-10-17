-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE IF NOT EXISTS localmachine.ack_records (
    eventid uuid PRIMARY KEY,
    sendto uuid NOT NULL,
    shipped BOOL NOT NULL DEFAULT FALSE
);

CREATE INDEX ON localmachine.ack_records(shipped,sendto);
