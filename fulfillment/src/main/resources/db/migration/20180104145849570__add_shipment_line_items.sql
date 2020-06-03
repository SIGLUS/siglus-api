CREATE TABLE shipment_line_items (
  id uuid PRIMARY KEY,
  orderableid uuid NOT NULL ,
  lotid uuid,
  quantityshipped bigint NOT NULL,
  shipmentid uuid NOT NULL
);

ALTER TABLE shipment_line_items
  ADD CONSTRAINT shipment_line_items_shipment_fk FOREIGN KEY (shipmentid) REFERENCES shipments(id);
