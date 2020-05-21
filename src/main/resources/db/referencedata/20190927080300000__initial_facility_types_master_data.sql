-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DELETE FROM "referencedata"."facility_types";

INSERT INTO "referencedata"."facility_types"("id","active","code","description","displayorder","name")
VALUES
('b6068140-cfad-11e9-9398-0242ac130008',TRUE,'CSRUR-I',NULL,1,'CS - Centro de Saúde Rural tipo 1'),
('b6069914-cfad-11e9-9398-0242ac130008',TRUE,'CSRUR-II',NULL,2,'CS - Centro de Saúde Rural tipo 2'),
('b6069ca2-cfad-11e9-9398-0242ac130008',TRUE,'DDM',NULL,3,'DDM - Depósito Distrital de Medicamentos'),
('b6069fa4-cfad-11e9-9398-0242ac130008',TRUE,'DPM',NULL,4,'DPM - Depósito Provincial de Medicamentos'),
('b606a26a-cfad-11e9-9398-0242ac130008',TRUE,'Central',NULL,5,'Moçambique'),
('b606a51c-cfad-11e9-9398-0242ac130008',TRUE,'HC',NULL,6,'HC - Hospital Central'),
('b606a7ba-cfad-11e9-9398-0242ac130008',TRUE,'HD',NULL,7,'HD - Hospital Distrital'),
('b606aa44-cfad-11e9-9398-0242ac130008',TRUE,'HG',NULL,8,'HG - Hospital Geral'),
('b606acc4-cfad-11e9-9398-0242ac130008',TRUE,'HP',NULL,9,'HP - Hospital Provincial'),
('b606af8a-cfad-11e9-9398-0242ac130008',TRUE,'HPSIQ',NULL,10,'HPSIQ - Hospital Psiquiatrico'),
('b606b214-cfad-11e9-9398-0242ac130008',TRUE,'HR',NULL,11,'HR - Hospital Rural'),
('b606b49e-cfad-11e9-9398-0242ac130008',TRUE,'PS',NULL,12,'PS - Posto de Saúde'),
('b606b71e-cfad-11e9-9398-0242ac130008',TRUE,'PSAPE',NULL,13,'PSAPE - Posto de Saúde APE'),
('b606b9a8-cfad-11e9-9398-0242ac130008',TRUE,'AC',NULL,14,'AC - Armazém Central'),
('b606bc5a-cfad-11e9-9398-0242ac130008',TRUE,'SI',NULL,15,'SI - Serviço Interno do Hospital'),
('b606bed0-cfad-11e9-9398-0242ac130008',TRUE,'HM',NULL,16,'HM - Hospital Militar'),
('b606c15a-cfad-11e9-9398-0242ac130008',TRUE,'ONG',NULL,17,'ONG - Organização não Governamental'),
('b606c3d0-cfad-11e9-9398-0242ac130008',TRUE,'OUTROS',NULL,18,'OUTROS'),
('b606c65a-cfad-11e9-9398-0242ac130008',TRUE,'AI',NULL,19,'AI - Armazém Intermediário'),
('b606c8d0-cfad-11e9-9398-0242ac130008',TRUE,'CSU-I',NULL,20,'CS - Centro de Saúde Urbano tipo 1'),
('b606cb50-cfad-11e9-9398-0242ac130008',TRUE,'CSU-II',NULL,21,'CS - Centro de Saúde Urbano tipo 2');
