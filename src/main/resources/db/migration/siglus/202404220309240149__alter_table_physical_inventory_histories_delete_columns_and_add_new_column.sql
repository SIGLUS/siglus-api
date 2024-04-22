-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
ALTER TABLE siglusintegration.physical_inventories_histories DROP COLUMN groupid;
ALTER TABLE siglusintegration.physical_inventories_histories DROP COLUMN physicalinventoryextensionid;

ALTER TABLE siglusintegration.physical_inventories_histories ADD COLUMN programid uuid NOT NULL;
ALTER TABLE siglusintegration.physical_inventories_histories ADD COLUMN completeddate DATE NOT NULL;
ALTER TABLE siglusintegration.physical_inventories_histories ADD COLUMN processdate TIMESTAMP WITH TIME ZONE NOT NULL;
