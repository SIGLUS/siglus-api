-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE IF NOT EXISTS localmachine.cdc_records
(
    id UUID PRIMARY KEY,
    txid BIGINT NOT NULL,
    tablename varchar(255) NOT NULL,
    schemaname varchar(255) NOT NULL,
    operationcode varchar(255) NOT NULL,
    capturedat TIMESTAMP WITH TIME ZONE NOT NULL,
    payload BYTEA NOT NULL
);

CREATE INDEX ON localmachine.cdc_records(txid);
