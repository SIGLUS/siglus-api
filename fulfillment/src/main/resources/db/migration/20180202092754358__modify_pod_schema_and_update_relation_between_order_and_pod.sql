-- connect external orders with pod
INSERT INTO fulfillment.shipments (id, orderid, shippedbyid, shippeddate, notes, extradata)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), o.id, o.createdById, o.createdDate, NULL, '{"external": true}'::jsonb
FROM fulfillment.proof_of_deliveries AS p
INNER JOIN fulfillment.orders AS o ON o.id = p.orderId;

INSERT INTO fulfillment.shipment_line_items (id, orderableid, lotid, quantityshipped, shipmentid)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), oli.orderableId, NULL, oli.orderedQuantity, s.id
FROM fulfillment.proof_of_deliveries AS p
INNER JOIN fulfillment.orders AS o ON o.id = p.orderId
INNER JOIN fulfillment.order_line_items AS oli ON o.id = oli.orderId
INNER JOIN fulfillment.shipments AS s ON o.id = s.orderId;

-- Since UPDATEs on large tables are not performant, we do this by creating a new table and copying the data.
-- See https://dba.stackexchange.com/questions/52517/best-way-to-populate-a-new-column-in-a-large-table/52531#52531
CREATE TABLE proof_of_deliveries_new AS 
SELECT pod.id
  , s.id AS shipmentid
  , CASE o.status 
      WHEN 'RECEIVED' THEN 'CONFIRMED'
      ELSE 'INITIATED'
    END AS status
  , pod.deliveredby
  , pod.receivedby
  , pod.receiveddate
FROM proof_of_deliveries pod
  JOIN orders o ON o.id = pod.orderid
  JOIN shipments s ON s.orderid = o.id
;

ALTER TABLE proof_of_deliveries_new
  ADD CONSTRAINT proofs_of_delivery_key PRIMARY KEY (id)
  , ALTER COLUMN shipmentid SET NOT NULL
  , ADD CONSTRAINT proofs_of_delivery_shipmentid_fk FOREIGN KEY (shipmentid) REFERENCES shipments(id)
  , ALTER COLUMN status SET DEFAULT 'INITIATED'
;

ALTER TABLE proof_of_delivery_line_items
  DROP CONSTRAINT proof_of_delivery_line_items_proofofdeliveryid_fk;

DROP TABLE proof_of_deliveries;
ALTER TABLE proof_of_deliveries_new RENAME TO proof_of_deliveries;

ALTER TABLE proof_of_delivery_line_items
  ADD CONSTRAINT proof_of_delivery_line_items_proofofdeliveryid_fk FOREIGN KEY (proofofdeliveryid) REFERENCES proof_of_deliveries(id);
