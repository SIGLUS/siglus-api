-- Move packstoship to orderedquantity.
-- Drop packstoship and filledquantity.
-- Since UPDATEs on large tables are not performant, we do this by creating a new table and copying the data.
-- See https://dba.stackexchange.com/questions/52517/best-way-to-populate-a-new-column-in-a-large-table/52531#52531

CREATE TABLE order_line_items_new AS 
SELECT id
  , orderid
  , orderableid
  , packstoship AS orderedquantity
FROM order_line_items
;

ALTER TABLE order_line_items_new
  ADD CONSTRAINT order_line_items_new_pkey PRIMARY KEY (id)
  , ALTER COLUMN orderid SET NOT NULL
  , ADD CONSTRAINT order_line_items_orderid_fk FOREIGN KEY (orderid) REFERENCES orders(id)
  , ALTER COLUMN orderableid SET NOT NULL
  , ALTER COLUMN orderedquantity SET NOT NULL
;

DROP TABLE order_line_items;
ALTER TABLE order_line_items_new RENAME TO order_line_items;

CREATE INDEX ON fulfillment.order_line_items (orderid);

ALTER TABLE order_line_items
  RENAME CONSTRAINT order_line_items_new_pkey TO order_line_items_pkey;
