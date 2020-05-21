-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."geographic_levels"("id","code","levelnumber","name")
VALUES
('ac8f43da-cfae-11e9-9398-0242ac130008','national',1,'National'),
('ac8f4dee-cfae-11e9-9398-0242ac130008','province',2,'Province'),
('ac8f50b4-cfae-11e9-9398-0242ac130008','district',3,'District');
