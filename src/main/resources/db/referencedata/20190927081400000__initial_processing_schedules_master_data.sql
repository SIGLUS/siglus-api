-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."processing_schedules"("id","code","description","modifieddate","name")
VALUES
('727bef28-de1c-11e9-8785-0242ac130007','M','Monthly','2019-09-23 16:08:57.266825+00','Monthly');
