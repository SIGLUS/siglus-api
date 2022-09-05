-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE IF NOT EXISTS localmachine.agents (
    machineid UUID NOT NULL PRIMARY KEY,
    facilityid UUID NOT NULL,
    facilitycode VARCHAR(255) NULL,
    privatekey TEXT NULL,
    publickey TEXT NULL,
    activationcode TEXT NULL,
    activatedat TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS localmachine.machine (
    touched BOOL UNIQUE DEFAULT true,
    id UUID NOT NULL PRIMARY KEY
    constraint chk_touched_flag CHECK (touched = true)
);

CREATE TABLE IF NOT EXISTS localmachine.activation_codes (
    id UUID NOT NULL PRIMARY KEY,
    facilitycode VARCHAR(255) NULL,
    activationcode TEXT NULL,
    used BOOL NOT NULL DEFAULT FALSE,
    usedat TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ON localmachine.activation_codes(used, facilitycode);