-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."roles"("id","description","name")
VALUES
('d8ff49b1-96e7-4710-9511-289f490f7577','Can view stock cards, edit stock inventories and adjust stock.','Stock Manager'),
('b6134c01-cb37-4303-85f0-8c8eab9d3cec','Can view, authorize and delete requisitions.','Store Manager'),
('185db8f7-ee35-44d0-8b40-6de12489ae77','Can create, view and delete requisitions and manage proofs of delivery.','Storeroom Manager');
