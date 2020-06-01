-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO digest_configurations (id, message, tag) VALUES
  ('4ec4f5fc-4397-4e98-bddd-2a2267cbe387', E'There are ${count} requisitions waiting for your approval\n${serviceUrl}/#!/requisitions/approvalList', 'requisition-actionRequired'),
  ('148f4acb-1504-41e9-ac71-c29e8599ee70', E'There are ${count} requisitions waiting for your converting to orders.\n${serviceUrl}/#!/requisitions/convertToOrder', 'requisition-requisitionApproved'),
  ('91290036-97b4-428c-8299-0ccc76bf3df3', E'There are ${count} requisitions have been converted to orders\n${serviceUrl}/#!/requisitions/view', 'requisition-convertToOrder'),
  ('407310f8-c44d-400c-93d1-c3ff7dd3a98f', E'There are ${count} requisitions with new statuses\n${serviceUrl}/#!/requisitions/view', 'requisition-statusUpdate');
