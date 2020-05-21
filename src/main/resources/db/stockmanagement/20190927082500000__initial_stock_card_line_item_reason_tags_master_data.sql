-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "stockmanagement"."stock_card_line_item_reason_tags"("reasonid","tag")
VALUES
('44814746-df64-11e9-9e7e-4c32759554d9','adjustment'),
('448147f0-df64-11e9-9e7e-4c32759554d9','adjustment'),
('448148b8-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44814a0c-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44814bc4-df64-11e9-9e7e-4c32759554d9','receive'),
('44814cc8-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44814e30-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44814f2a-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44814fde-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44815088-df64-11e9-9e7e-4c32759554d9','adjustment'),
('4481515a-df64-11e9-9e7e-4c32759554d9','adjustment'),
('44815222-df64-11e9-9e7e-4c32759554d9','adjustment'),
('448152fe-df64-11e9-9e7e-4c32759554d9','adjustment'),
('448153bc-df64-11e9-9e7e-4c32759554d9','issue');
