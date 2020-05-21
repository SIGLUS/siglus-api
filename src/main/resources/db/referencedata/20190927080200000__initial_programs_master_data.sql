-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."programs"("id","active","code","description","name","periodsskippable","shownonfullsupplytab","enabledatephysicalstockcountcompleted","skipauthorization")
VALUES
('10845cb9-d365-4aaa-badd-b4fa39c6a26a',TRUE,'ARVP',NULL,'ARV',FALSE,FALSE,FALSE,FALSE),
('a24f19a8-3743-4a1a-a919-e8f97b5719ad',TRUE,'RTP',NULL,'Rapid Test',FALSE,FALSE,FALSE,FALSE),
('dce17f2e-af3e-40ad-8e00-3496adef44c3',TRUE,'MP',NULL,'Multiple Programs',FALSE,FALSE,FALSE,FALSE),
('ad8c9a54-cc66-4395-a353-1849caec87da',TRUE,'ALP',NULL,'AL',FALSE,FALSE,FALSE,FALSE),
('f7f82c00-cfd2-11e9-9535-0242ac130005',TRUE,'PT',NULL,'PTV',FALSE,FALSE,FALSE,FALSE),
('dfbbe370-cfd2-11e9-9535-0242ac130005',TRUE,'MMC',NULL,'Material Medico  Cirugico',FALSE,FALSE,FALSE,FALSE),
('eae5b88e-cfd2-11e9-9535-0242ac130005',TRUE,'TR',NULL,'Testes Rápidos Diag.',FALSE,FALSE,FALSE,FALSE),
('d5880ac8-cfd2-11e9-9535-0242ac130005',TRUE,'AI',NULL,'Auditoria Interna',FALSE,FALSE,FALSE,FALSE),
('d5881f5e-cfd2-11e9-9535-0242ac130005',TRUE,'MA',NULL,'Material de Armazenagem',FALSE,FALSE,FALSE,FALSE),
('dfbbec62-cfd2-11e9-9535-0242ac130005',TRUE,'MOD',NULL,'Modelos',FALSE,FALSE,FALSE,FALSE),
('dfbbf522-cfd2-11e9-9535-0242ac130005',TRUE,'P',NULL,'PME',FALSE,FALSE,FALSE,FALSE),
('dfbbd880-cfd2-11e9-9535-0242ac130005',TRUE,'ML',NULL,'Malaria',FALSE,FALSE,FALSE,FALSE),
('eae5ab5a-cfd2-11e9-9535-0242ac130005',TRUE,'T',NULL,'TARV',FALSE,FALSE,FALSE,FALSE),
('d58815d6-cfd2-11e9-9535-0242ac130005',TRUE,'M',NULL,'Medicamentos Essenciais',FALSE,FALSE,FALSE,FALSE),
('8746258a-de1d-11e9-8785-0242ac130007',TRUE,'N',NULL,'NUTRITION',FALSE,FALSE,FALSE,FALSE),
('eae5b1ea-cfd2-11e9-9535-0242ac130005',TRUE,'TB',NULL,'Tuberculose',FALSE,FALSE,FALSE,FALSE);
