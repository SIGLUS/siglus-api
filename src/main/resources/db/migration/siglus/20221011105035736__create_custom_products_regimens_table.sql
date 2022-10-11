-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.custom_products_regimens (
    id uuid PRIMARY KEY,
    code character varying(255) UNIQUE,
    programcode character varying(255),
    categorytype character varying(255),
    type character varying(255),
    iscustom boolean
);
