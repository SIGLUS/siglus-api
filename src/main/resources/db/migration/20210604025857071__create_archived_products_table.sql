-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.archived_products (
    id uuid PRIMARY KEY,
    facilityid uuid NOT NULL,
    orderableid uuid NOT NULL
);

CREATE INDEX archived_products_facilityid_idx ON siglusintegration.archived_products(facilityid uuid_ops);
CREATE UNIQUE INDEX archived_products_facilityid_orderableid_idx ON siglusintegration.archived_products(facilityid uuid_ops,orderableid uuid_ops);
