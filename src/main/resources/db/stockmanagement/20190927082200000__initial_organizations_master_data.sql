-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "stockmanagement"."organizations"("id","name")
VALUES
('4481207c-df64-11e9-9e7e-4c32759554d9','UNPACK'),
('448126d0-df64-11e9-9e7e-4c32759554d9','UATS'),
('4481281a-df64-11e9-9e7e-4c32759554d9','Public pharmacy'),
('448128ba-df64-11e9-9e7e-4c32759554d9','Province(DPM)'),
('44812928-df64-11e9-9e7e-4c32759554d9','PNCTL'),
('448129a0-df64-11e9-9e7e-4c32759554d9','PAV'),
('44812a04-df64-11e9-9e7e-4c32759554d9','Mobile unit'),
('44812a5e-df64-11e9-9e7e-4c32759554d9','Maternity'),
('44812b1c-df64-11e9-9e7e-4c32759554d9','Laboratory'),
('44812be4-df64-11e9-9e7e-4c32759554d9','General Ward'),
('44812c48-df64-11e9-9e7e-4c32759554d9','District(DDM)'),
('44812cb6-df64-11e9-9e7e-4c32759554d9','Dental ward'),
('44812e3c-df64-11e9-9e7e-4c32759554d9','Accident & Emergency'),
('44815934-df64-11e9-9e7e-4c32759554d9','KIT PME US; Sem Dosagem; KIT'),
('448159f2-df64-11e9-9e7e-4c32759554d9','KIT AL/US (Artemeter+Lumefantrina); 170 Tratamentos+ 400 Testes; KIT');
