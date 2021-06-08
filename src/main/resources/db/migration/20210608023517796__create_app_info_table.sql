-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE siglusintegration.app_info (
    id uuid NOT NULL,
    facilitycode VARCHAR(20) NOT NULL,
    facilityname VARCHAR(100) NOT NULL,
    uniqueid  VARCHAR(40) NOT NULL,
    deviceinfo VARCHAR(200) NOT NULL,
    versioncode INTEGER,
    androidsdkversion INTEGER,
    username VARCHAR(100) NOT NULL,
    lastupdated TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_info_pkey PRIMARY KEY (facilitycode,uniqueid)
);

CREATE INDEX ON siglusintegration.app_info (facilitycode, uniqueid);