-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.stock_management_initial_drafts
(
    id UUID PRIMARY KEY NOT NULL,
    facilityid UUID NOT NULL,
    programid UUID NOT NULL,
    documentnumber VARCHAR(255),
    destinationid UUID,
    sourceid UUID,
    locationfreetext VARCHAR(255),
    createdtime TIMESTAMP,
    drafttype VARCHAR(20)
);

ALTER TABLE siglusintegration.stock_management_drafts
    ADD COLUMN initialdraftid UUID,
    ADD COLUMN status VARCHAR(30),
    ADD COLUMN operator VARCHAR(30);