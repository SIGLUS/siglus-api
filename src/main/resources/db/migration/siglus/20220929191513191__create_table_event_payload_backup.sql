-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP TABLE IF EXISTS localmachine.event_payload;
CREATE TABLE localmachine.event_payload
(
    eventid uuid PRIMARY KEY,
    payload bytea
);
DROP TABLE IF EXISTS localmachine.event_payload_backup;
CREATE TABLE localmachine.event_payload_backup
(
    eventid uuid PRIMARY KEY,
    payload bytea
);

INSERT INTO localmachine.event_payload (eventid, payload) (SELECT id, payload FROM localmachine.events);

ALTER TABLE localmachine.events
    ADD COLUMN archived boolean NOT NULL DEFAULT FALSE;
ALTER TABLE localmachine.events
    DROP COLUMN payload;