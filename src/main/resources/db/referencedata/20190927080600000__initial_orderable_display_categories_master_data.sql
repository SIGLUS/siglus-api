-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."orderable_display_categories"("id","code","displayname","displayorder")
VALUES
('da7e4266-de20-11e9-8785-0242ac130007','11','Other',1),
('da7e7556-de20-11e9-8785-0242ac130007','12','Adult',2),
('da7e7baa-de20-11e9-8785-0242ac130007','13','Children',3),
('da7e7fba-de20-11e9-8785-0242ac130007','14','Solution',4);

