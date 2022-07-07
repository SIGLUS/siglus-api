-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.regimen_summary_line_items RENAME COLUMN regimendispatchlineid TO name;
ALTER TABLE siglusintegration.regimen_summary_line_item_draft RENAME COLUMN regimendispatchlineid TO name;
ALTER TABLE siglusintegration.regimen_summary_line_items ALTER COLUMN name TYPE VARCHAR(255);
ALTER TABLE siglusintegration.regimen_summary_line_item_draft ALTER COLUMN name TYPE VARCHAR(255);

ALTER TABLE siglusintegration.regimen_line_items ALTER COLUMN regimenId DROP NOT NULL;
ALTER TABLE siglusintegration.regimen_line_item_draft ALTER COLUMN regimenId DROP NOT NULL;
