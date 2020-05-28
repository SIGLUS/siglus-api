-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DELETE FROM "siglusintegration"."program_extension" WHERE "id" IN ('dfbbec62-cfd2-11e9-9536-0242ac130005','dfbbe370-cfd2-11e9-9536-0242ac130005','d5881f5e-cfd2-11e9-9536-0242ac130005','d5880ac8-cfd2-11e9-9536-0242ac130005');