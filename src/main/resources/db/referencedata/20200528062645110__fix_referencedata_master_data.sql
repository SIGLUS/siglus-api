-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- remove 6 no-used facilities
DELETE FROM "referencedata"."supported_programs" WHERE facilityid IN ('c149b456-cfbb-11e9-9398-0242ac130008','0591e6fa-cfbd-11e9-9398-0242ac130008','c19d2e68-cfb8-11e9-9398-0242ac130008','05926eae-cfbd-11e9-9398-0242ac130008','c19d8f5c-cfb8-11e9-9398-0242ac130008','795ef9c0-cfbc-11e9-9398-0242ac130008');
DELETE FROM "referencedata"."facilities" WHERE id IN ('c149b456-cfbb-11e9-9398-0242ac130008','0591e6fa-cfbd-11e9-9398-0242ac130008','c19d2e68-cfb8-11e9-9398-0242ac130008','05926eae-cfbd-11e9-9398-0242ac130008','c19d8f5c-cfb8-11e9-9398-0242ac130008','795ef9c0-cfbc-11e9-9398-0242ac130008');

-- update geographiczoneid of facility "DPM TETE"
UPDATE "referencedata"."facilities" SET "geographiczoneid"='3f2e1b30-cfaf-11e9-9398-0242ac130008' WHERE "id"='004f4232-cfb8-11e9-9398-0242ac130008';

-- add supervisory_nodes for 19 facilities
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('ec28dbfa-9f65-11ea-b0b3-0242ac130006','N19','Approval Point Quelimane','Approval Point Quelimane','05918674-cfbd-11e9-9398-0242ac130008','642804ec-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('181df90c-9f66-11ea-b0b3-0242ac130006','N70','Approval Point Inhassunge','Approval Point Inhassunge','05922476-cfbd-11e9-9398-0242ac130008','642804ec-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('11cc99c8-9f66-11ea-b0b3-0242ac130006','N46','Approval Point Pemba','Approval Point Pemba','060a41c8-cfbc-11e9-9398-0242ac130008','64277cac-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('28208626-9f66-11ea-b0b3-0242ac130006','N81','Approval Point Distrito de Inhambane','Approval Point Distrito de Inhambane','1ac4d19c-cfbb-11e9-9398-0242ac130008','642838ae-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('2d6b1420-9f6b-11ea-b0b3-0242ac130006','N112','Approval Point Mandlakaze','Approval Point Mandlakaze','1ac50c48-cfbb-11e9-9398-0242ac130008','642849a2-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('ec29d9ce-9f65-11ea-b0b3-0242ac130006','N22','Approval Point Xai-Xai','Approval Point Xai-Xai','1ac51fa8-cfbb-11e9-9398-0242ac130008','642849a2-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('312530d2-9f66-11ea-b0b3-0242ac130006','N105','Approval Point Metuge','Approval Point Metuge','3ecf8206-cfbd-11e9-9398-0242ac130008','64277cac-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('181e074e-9f66-11ea-b0b3-0242ac130006','N73','Approval Point Nacala Porto','Approval Point Nacala Porto','3ecf8c92-cfbd-11e9-9398-0242ac130008','6427f2b8-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('181e11f8-9f66-11ea-b0b3-0242ac130006','N74','Approval Point Nacala-a-velha','Approval Point Nacala-a-velha','3ecf9674-cfbd-11e9-9398-0242ac130008','6427f2b8-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('11ccb80e-9f66-11ea-b0b3-0242ac130006','N69','Approval Point Rapale','Approval Point Rapale','3ecfab1e-cfbd-11e9-9398-0242ac130008','6427f2b8-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('2d6b2532-9f6b-11ea-b0b3-0242ac130006','N113','Approval Point Tsangano','Approval Point Tsangano','4d2e7466-cfbc-11e9-9398-0242ac130008','64281662-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('2d6b3180-9f6b-11ea-b0b3-0242ac130006','N114','Approval Point Cidade de Tete','Approval Point Cidade de Tete','60bc12ba-cfba-11e9-9398-0242ac130008','64281662-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('31253eba-9f66-11ea-b0b3-0242ac130006','N17','Approval Point Angonia','Approval Point Angonia','60bc26ec-cfba-11e9-9398-0242ac130008','64281662-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('ec29e7c0-9f65-11ea-b0b3-0242ac130006','N43','Approval Point Chokwe','Approval Point Chokwe','673f2174-cfb8-11e9-9398-0242ac130008','642849a2-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('2820910c-9f66-11ea-b0b3-0242ac130006','N85','Approval Point Erati','Approval Point Erati','a961ef64-cfb8-11e9-9398-0242ac130008','6427f2b8-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('11ccaae4-9f66-11ea-b0b3-0242ac130006','N49','Approval Point Beira','Approval Point Beira','aa8076cc-cfbd-11e9-9398-0242ac130008','64282738-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('28207820-9f66-11ea-b0b3-0242ac130006','N80','Approval Point Maxixe','Approval Point Maxixe','c19d5e56-cfb8-11e9-9398-0242ac130008','642838ae-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('0fc9c4bc-9f6a-11ea-b0b3-0242ac130006','N111','Approval Point Cidade de Xai-Xai','Approval Point Cidade de Xai-Xai','c19daf14-cfb8-11e9-9398-0242ac130008','642849a2-daf8-11e9-9f38-0242ac130009',NULL,NULL);
INSERT INTO "referencedata"."supervisory_nodes"("id","code","description","name","facilityid","parentid","extradata","partnerid") VALUES ('2d6b6286-9f6b-11ea-b0b3-0242ac130006','N115','Approval Point Cidade de Nampula','Approval Point Cidade de Nampula','c224bb34-cfb9-11e9-9398-0242ac130008','6427f2b8-daf8-11e9-9f38-0242ac130009',NULL,NULL);

-- delete 4 no-used programs
DELETE FROM "referencedata"."supported_programs" WHERE programid IN ('d5880ac8-cfd2-11e9-9535-0242ac130005','d5881f5e-cfd2-11e9-9535-0242ac130005','dfbbe370-cfd2-11e9-9535-0242ac130005','dfbbec62-cfd2-11e9-9535-0242ac130005');
DELETE FROM "referencedata"."programs" WHERE "id" IN ('d5880ac8-cfd2-11e9-9535-0242ac130005','d5881f5e-cfd2-11e9-9535-0242ac130005','dfbbe370-cfd2-11e9-9535-0242ac130005','dfbbec62-cfd2-11e9-9535-0242ac130005');

-- update orderable name and code for "Gonadotrofina coriónica humana"
UPDATE "referencedata"."orderables" SET "fullproductname"='Gonadotrofina coriónica humana5000UI/mLInjectável', "code"='04E010' WHERE "id"='76af44e8-d956-11e9-944f-0242ac130005';

--  asscoiate orderable with program
INSERT INTO "referencedata"."program_orderables"("id","active","displayorder","dosesperpatient","fullsupply","priceperpack","orderabledisplaycategoryid","orderableid","programid","orderableversionnumber") VALUES ('02236dbe-8767-4bc8-bf48-63336a68cab1',TRUE,1,NULL,TRUE,0,'da7e4266-de20-11e9-8785-0242ac130007','76af44e8-d956-11e9-944f-0242ac130005','d58815d6-cfd2-11e9-9535-0242ac130005',1);
INSERT INTO "referencedata"."program_orderables"("id","active","displayorder","dosesperpatient","fullsupply","priceperpack","orderabledisplaycategoryid", "orderableid", "programid", "orderableversionnumber") VALUES ('44725005-71b3-41df-9eae-a06414a995f3',TRUE,1,NULL,TRUE,0,'da7e4266-de20-11e9-8785-0242ac130007','a7b7f2d8-d956-11e9-944f-0242ac130005','d58815d6-cfd2-11e9-9535-0242ac130005', 1);

-- remove requisition_groups "VIA Requisition Group 68"
DELETE FROM "referencedata"."requisition_group_program_schedules" WHERE "requisitiongroupid"='c037f210-de1d-11e9-8785-0242ac130007';
DELETE FROM "referencedata"."requisition_groups" WHERE "id"='c037f210-de1d-11e9-8785-0242ac130007';

-- update requisition_groups name and description
UPDATE "referencedata"."requisition_groups" SET "name"=replace(description,'VIA','Via'), "description"=replace(description,'VIA','Via');

-- update requisition_groups code
UPDATE "referencedata"."requisition_groups" SET "code"='RG83' WHERE "id"='668212ca-de20-11e9-8785-0242ac130007';
UPDATE "referencedata"."requisition_groups" SET "code"='RG82' WHERE "id"='6f3c105a-de20-11e9-8785-0242ac130007';
