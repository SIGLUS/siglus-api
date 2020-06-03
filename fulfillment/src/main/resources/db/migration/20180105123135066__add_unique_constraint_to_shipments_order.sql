ALTER TABLE shipments
  ADD CONSTRAINT shipments_order_unq UNIQUE (orderid);
