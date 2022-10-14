-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP TABLE IF EXISTS localmachine.last_sync_replay_record;
CREATE TABLE localmachine.last_sync_replay_record
(
    id               uuid PRIMARY KEY,
    lastsyncedtime   TIMESTAMP WITH TIME ZONE NOT NULL,
    lastreplayedtime TIMESTAMP WITH TIME ZONE NOT NULL
);

DROP TABLE IF EXISTS localmachine.error_records;
CREATE TABLE localmachine.error_records
(
    id             uuid PRIMARY KEY,
    errorpayloadid uuid                     NOT NULL,
    type           varchar(255),
    occurredtime   TIMESTAMP WITH TIME ZONE NOT NULL
);

DROP TABLE IF EXISTS localmachine.error_payloads;
CREATE TABLE localmachine.error_payloads
(
    id             uuid PRIMARY KEY,
    eventid        uuid,
    rootstacktrace text,
    errorname      varchar(255),
    messagekey     varchar(255),
    detailmessage  text
);
ALTER TABLE localmachine.error_records
    ADD FOREIGN KEY (errorpayloadid) REFERENCES localmachine.error_payloads (id);


create
index error_records_index on localmachine.error_records (occurredtime);