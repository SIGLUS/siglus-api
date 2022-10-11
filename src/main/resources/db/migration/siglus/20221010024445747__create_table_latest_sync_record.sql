-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP TABLE IF EXISTS localmachine.latest_sync_replay_record;
CREATE TABLE localmachine.latest_sync_replay_record
(
    id uuid PRIMARY KEY,
    latestsyncedtime TIMESTAMP WITH TIME ZONE NOT NULL,
    latestreplayedtime TIMESTAMP WITH TIME ZONE NOT NULL
);

DROP TABLE IF EXISTS localmachine.replay_error_records;
CREATE TABLE localmachine.replay_error_records
(
    id uuid PRIMARY KEY,
    errors bytea,
    type varchar(255),
    occurredtime TIMESTAMP WITH TIME ZONE NOT NULL
);