-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.pod_confirm_backup
(
    id          UUID PRIMARY KEY,
    ordernumber CHARACTER VARYING(255) NOT NULL,
    facilityid  UUID                   NOT NULL,
    oldpod      jsonb,
    newpod      jsonb,
    createddate TIMESTAMP WITH TIME ZONE
);