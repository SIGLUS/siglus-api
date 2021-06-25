-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.product_requests(
  id UUID PRIMARY KEY,
  orderableid UUID NOT NULL,
  stockeventid UUID NOT NULL,
  requestquantity INTEGER
);

CREATE UNIQUE INDEX product_requests_orderableid_stockeventid_idx ON siglusintegration.product_requests(orderableid, stockeventid);
