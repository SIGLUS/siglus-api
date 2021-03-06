-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.cmms
(
    id uuid PRIMARY KEY,
    facilitycode VARCHAR(255) NOT NULL,
    facilityname VARCHAR(255) NOT NULL,
    productcode VARCHAR(255) NOT NULL,
    productname VARCHAR(255) NOT NULL,
    realprogramcode VARCHAR(255) NOT NULL,
    realprogramname VARCHAR(255) NOT NULL,
    cmm INTEGER,
    max INTEGER,
    period VARCHAR(20) NOT NULL,
    year INTEGER,
    querydate VARCHAR(20) NOT NULL,
    date TIMESTAMP WITH TIME ZONE,
    lastupdatedat TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ON siglusintegration.cmms (facilitycode, productcode, querydate);