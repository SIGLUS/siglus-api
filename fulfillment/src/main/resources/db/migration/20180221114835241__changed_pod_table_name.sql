ALTER TABLE ONLY proof_of_deliveries RENAME TO proofs_of_delivery;

CREATE INDEX ON fulfillment.proofs_of_delivery (shipmentid);