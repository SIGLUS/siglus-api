CREATE TABLE shipments (
  id uuid PRIMARY KEY,
  orderid uuid,
  shippedbyid uuid NOT NULL,
  shippeddate timestamp with time zone NOT NULL,
  notes text
);

ALTER TABLE shipments
  ADD CONSTRAINT shipments_orderid_fk FOREIGN KEY (orderid) REFERENCES orders(id);
