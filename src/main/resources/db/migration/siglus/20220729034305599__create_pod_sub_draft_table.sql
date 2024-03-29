-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.pod_sub_draft
(
    id                UUID    NOT NULL PRIMARY KEY,
    number            INTEGER NOT NULL,
    status            INTEGER,
    operatorid        UUID,
    proofofdeliveryid UUID
);

CREATE UNIQUE INDEX podid_num_uidx ON siglusintegration.pod_sub_draft (proofofdeliveryid, number);