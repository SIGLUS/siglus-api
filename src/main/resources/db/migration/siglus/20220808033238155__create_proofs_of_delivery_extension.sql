-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.proofs_of_delivery_extension
(
    id                UUID NOT NULL PRIMARY KEY,
    proofofdeliveryid UUID NOT NULL,
    preparedby varchar(255) NULL,
    conferredby varchar(255) NULL
);

CREATE UNIQUE INDEX ON siglusintegration.proofs_of_delivery_extension (proofofdeliveryid);