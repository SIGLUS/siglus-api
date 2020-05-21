-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."dispensable_attributes"("dispensableid","key","value")
VALUES
('d397114e-d39e-11e9-8e36-0242ac130003','dispensingUnit','each');
