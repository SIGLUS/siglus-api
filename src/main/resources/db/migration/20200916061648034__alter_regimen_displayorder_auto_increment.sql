-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.regimens ALTER COLUMN dispatchlineid DROP NOT NULL;
ALTER TABLE siglusintegration.regimens ALTER COLUMN active DROP NOT NULL;
ALTER TABLE siglusintegration.regimens ALTER COLUMN iscustom DROP NOT NULL;
ALTER TABLE siglusintegration.regimens ALTER COLUMN programid DROP NOT NULL;
