-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE siglusintegration.hf_cmms (
    id uuid PRIMARY KEY,
    facilitycode VARCHAR(50) NOT NULL,
    productcode VARCHAR(50) NOT NULL,
    periodBegin DATE NOT NULL,
    periodEnd DATE NOT NULL,
    cmm DOUBLE PRECISION,
    lastupdated TIMESTAMP WITH TIME ZONE
);

CREATE INDEX hf_cmms_facilitycode_idx ON siglusintegration.hf_cmms(facilitycode);
CREATE UNIQUE INDEX facilitycode_uniqueid_begin_end_idx ON siglusintegration.hf_cmms (facilitycode, productcode, periodBegin, periodEnd);