-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO siglusintegration.program_extension("id","programid","code","name","parentid","isvirtual","issupportemergency")
VALUES
('dce17f2e-af3e-40ad-8e06-3496adef44c3','dce17f2e-af3e-40ad-8e00-3496adef44c3','MP','Multiple Programs',NULL,TRUE,TRUE),
('10845cb9-d365-4aaa-bad6-b4fa39c6a26a','10845cb9-d365-4aaa-badd-b4fa39c6a26a','ARVP','ARV',NULL,TRUE,FALSE),
('a24f19a8-3743-4a1a-a916-e8f97b5719ad','a24f19a8-3743-4a1a-a919-e8f97b5719ad','RTP','Raipd Test',NULL,TRUE,FALSE),
('ad8c9a54-cc66-4395-a356-1849caec87da','ad8c9a54-cc66-4395-a353-1849caec87da','ALP','AL',NULL,TRUE,FALSE),
('f7f82c00-cfd2-11e9-9536-0242ac130005','f7f82c00-cfd2-11e9-9535-0242ac130005','PT','PTV','10845cb9-d365-4aaa-badd-b4fa39c6a26a',FALSE,FALSE),
('dfbbe370-cfd2-11e9-9536-0242ac130005','dfbbe370-cfd2-11e9-9535-0242ac130005','MMC','Material Medico  Cirugico','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('eae5b88e-cfd2-11e9-9536-0242ac130005','eae5b88e-cfd2-11e9-9535-0242ac130005','TR','Testes Rápidos Diag.','a24f19a8-3743-4a1a-a919-e8f97b5719ad',FALSE,FALSE),
('d5880ac8-cfd2-11e9-9536-0242ac130005','d5880ac8-cfd2-11e9-9535-0242ac130005','AI','Auditoria Interna','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('d5881f5e-cfd2-11e9-9536-0242ac130005','d5881f5e-cfd2-11e9-9535-0242ac130005','MA','Material de Armazenagem','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('dfbbec62-cfd2-11e9-9536-0242ac130005','dfbbec62-cfd2-11e9-9535-0242ac130005','MOD','Modelos','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('dfbbf522-cfd2-11e9-9536-0242ac130005','dfbbf522-cfd2-11e9-9535-0242ac130005','P','PME','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('dfbbd880-cfd2-11e9-9536-0242ac130005','dfbbd880-cfd2-11e9-9535-0242ac130005','ML','Malaria','ad8c9a54-cc66-4395-a353-1849caec87da',FALSE,FALSE),
('eae5ab5a-cfd2-11e9-9536-0242ac130005','eae5ab5a-cfd2-11e9-9535-0242ac130005','T','TARV','10845cb9-d365-4aaa-badd-b4fa39c6a26a',FALSE,FALSE),
('d58815d6-cfd2-11e9-9536-0242ac130005','d58815d6-cfd2-11e9-9535-0242ac130005','M','Medicamentos Essenciais','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('8746258a-de1d-11e9-8786-0242ac130007','8746258a-de1d-11e9-8785-0242ac130007','N','NUTRITION','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE),
('eae5b1ea-cfd2-11e9-9536-0242ac130005','eae5b1ea-cfd2-11e9-9535-0242ac130005','TB','Tuberculose','dce17f2e-af3e-40ad-8e00-3496adef44c3',FALSE,FALSE);
