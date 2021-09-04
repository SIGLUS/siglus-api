-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP TABLE siglusintegration.facility_extension;

CREATE TABLE siglusintegration.facility_extension(
  id UUID PRIMARY KEY,
  facilityid UUID UNIQUE NOT NULL,
  facilitycode VARCHAR(255) UNIQUE NOT NULL,
  v2facilitycode VARCHAR(255),
  isandroid BOOLEAN NOT NULL
);

CREATE INDEX ON siglusintegration.facility_extension(v2facilitycode);
