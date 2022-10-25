-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP TABLE IF EXISTS siglusintegration.generated_number;

CREATE TABLE siglusintegration.generated_number (
    id uuid PRIMARY KEY,
    facilityid uuid NOT NULL,
    programid uuid NOT NULL,
    year INTEGER NOT NULL,
    emergency BOOLEAN DEFAULT false,
    number INTEGER
);

CREATE UNIQUE INDEX ON siglusintegration.generated_number(facilityid, programid, year, emergency);
