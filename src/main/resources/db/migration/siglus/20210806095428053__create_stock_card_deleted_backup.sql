-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.stock_card_deleted_backup
(
    id              uuid PRIMARY KEY,
    facilityid      uuid  NOT NULL,
    productid       uuid  NOT NULL,
    servermovements jsonb,
    clientmovements jsonb NOT NULL,
    createdby       uuid,
    createddate     timestamp with time zone
);

CREATE
UNIQUE INDEX unq_id ON siglusintegration.stock_card_deleted_backup(id uuid_ops);
CREATE
INDEX index_facilityid_productid ON siglusintegration.stock_card_deleted_backup(facilityid uuid_ops,productid uuid_ops);
