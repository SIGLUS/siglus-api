-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE SEQUENCE localmachine.event_seq;
CREATE TABLE localmachine.events (
    sequence_number BIGINT NOT NULL DEFAULT nextval('localmachine.event_seq') PRIMARY KEY
);
ALTER SEQUENCE localmachine.event_seq OWNED BY localmachine.events.sequence_number;