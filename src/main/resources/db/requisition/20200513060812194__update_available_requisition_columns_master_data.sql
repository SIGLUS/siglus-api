-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

UPDATE requisition.available_requisition_columns SET label = 'Product Name' WHERE name ='orderable.fullProductName';
UPDATE requisition.available_requisition_columns SET label = 'Stock at Beginning of Period' WHERE name ='beginningBalance';
UPDATE requisition.available_requisition_columns SET label = 'Sum of Entries' WHERE name ='totalReceivedQuantity';
UPDATE requisition.available_requisition_columns SET label = 'Issues' WHERE name ='totalConsumedQuantity';
UPDATE requisition.available_requisition_columns SET label = 'Inventory'  WHERE name ='stockOnHand';
UPDATE requisition.available_requisition_columns SET label = 'Quantity Approved' WHERE name ='approvedQuantity';

UPDATE requisition.available_requisition_columns SET isdisplayrequired = TRUE WHERE name = 'skipped';
