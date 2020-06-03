CREATE TABLE shipment_drafts (
  id uuid PRIMARY KEY,
  orderid uuid,
  notes text
);

CREATE TABLE shipment_draft_line_items (
  id uuid PRIMARY KEY,
  orderableid uuid NOT NULL,
  lotid uuid,
  quantityshipped bigint,
  shipmentdraftid uuid NOT NULL
);

ALTER TABLE shipment_drafts
  ADD CONSTRAINT shipment_drafts_orderid_fk FOREIGN KEY (orderid) REFERENCES orders(id);

ALTER TABLE shipment_drafts
ADD CONSTRAINT shipment_drafts_orderid_unq UNIQUE (orderid);

ALTER TABLE shipment_draft_line_items
ADD CONSTRAINT shipment_draft_line_items_shipmentdraftid_fk FOREIGN KEY (shipmentdraftid) REFERENCES shipment_drafts(id);
