-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.cps
(
    id uuid PRIMARY KEY,
    facilitycode text NOT NULL,
    facilityname text NOT NULL,
    productcode text NOT NULL,
    productname text NOT NULL,
    realprogramcode text NOT NULL,
    realprogramname text NOT NULL,
    cp INTEGER,
    max INTEGER,
    period text NOT NULL,
    year INTEGER,
    date TIMESTAMP WITH TIME ZONE,
    lastupdatedat TIMESTAMP WITH TIME ZONE
)