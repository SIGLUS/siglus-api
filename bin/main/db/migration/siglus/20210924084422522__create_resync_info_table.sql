-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE siglusintegration.resync_info (
    id UUID PRIMARY KEY,
    facilityid UUID NOT NULL,
    facilityname VARCHAR(255) NOT NULL,
    uniqueid  VARCHAR(255),
    deviceinfo VARCHAR(255),
    versioncode VARCHAR(255),
    androidsdkversion VARCHAR(255),
    userid UUID NOT NULL,
    username VARCHAR(255),
    resyncTime TIMESTAMP WITH TIME ZONE
);