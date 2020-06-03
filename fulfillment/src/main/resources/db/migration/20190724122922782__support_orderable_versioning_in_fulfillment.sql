-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE fulfillment.shipment_line_items
  ADD COLUMN orderableVersionNumber bigint;

ALTER TABLE fulfillment.shipment_draft_line_items
  ADD COLUMN orderableVersionNumber bigint;

ALTER TABLE fulfillment.order_line_items
  ADD COLUMN orderableVersionNumber bigint;

ALTER TABLE fulfillment.proof_of_delivery_line_items
  ADD COLUMN orderableVersionNumber bigint;

UPDATE fulfillment.shipment_line_items
  SET orderableVersionNumber = 1;

UPDATE fulfillment.shipment_draft_line_items
  SET orderableVersionNumber = 1;

UPDATE fulfillment.proof_of_delivery_line_items
  SET orderableVersionNumber = 1;

UPDATE fulfillment.file_columns
SET
    keypath = 'id', nested = 'lineItemOrderable'
WHERE
    id = '752cda76-0db5-4b6e-bb79-0f531ab78e2e';

UPDATE fulfillment.file_columns
SET
    keypath = 'id', nested = 'lineItemOrderable'
WHERE
    id = '9e825396-269d-4873-baa4-89054e2722f5';

UPDATE fulfillment.file_columns
SET
    keypath = 'id', nested = 'lineItemOrderable'
WHERE
    id = 'aea50901-5ad5-49f2-89ce-43ee55624e38';
