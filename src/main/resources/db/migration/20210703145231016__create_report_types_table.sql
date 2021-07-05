-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.reporttypes(
  id UUID PRIMARY KEY,
  facilityid UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  programcode VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL,
  startdate DATE
);

CREATE INDEX siglusintegration_reporttypes_idx ON siglusintegration.reporttypes(facilityid);