-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.receipt_plans (
    id uuid PRIMARY KEY,
    receiptplannumber CHARACTER VARYING(255) UNIQUE,
    facilitycode CHARACTER VARYING(255),
    facilityname CHARACTER VARYING(255),
    approverequisitiondate TIMESTAMP WITH TIME ZONE,
    requisitionnumber CHARACTER VARYING(255),
    lastupdateddate TIMESTAMP WITH TIME ZONE
)
