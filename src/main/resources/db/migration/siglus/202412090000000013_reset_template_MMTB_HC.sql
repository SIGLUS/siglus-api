-- MMTB Template - HC
SET local vars.template = 'MMTB Template - HC';
SET local vars.templateid = 'c3f54e3e-2fff-449b-a870-08b6ad7b7b32';
SET local vars.programid = 'bff50392-0a46-4da3-8adc-d47a37fb6a9f';

-- insert or update template
INSERT INTO requisition.requisition_templates (id,createddate,modifieddate,numberofperiodstoaverage,populatestockonhandfromstockcards,archived,name)
    VALUES (current_setting('vars.templateid')::uuid,'2022-08-08 08:56:14.443+00',now(),3,TRUE,FALSE,current_setting('vars.template'))
ON CONFLICT DO NOTHING;

-- fix historical data
UPDATE requisition.requisitions
SET templateid = current_setting('vars.templateid')::uuid
WHERE programid = current_setting('vars.programid')::uuid
    AND facilityid IN (
        SELECT f.id FROM referencedata.facilities f JOIN referencedata.facility_types ft ON ft.id = f.typeid
        WHERE ft.code IN ('HC')
    );

-- delete template sections
DELETE FROM siglusintegration.usage_columns_maps WHERE requisitiontemplateid IN (SELECT id FROM requisition.requisition_templates WHERE name = current_setting('vars.template'));
DELETE FROM siglusintegration.usage_sections_maps WHERE requisitiontemplateid IN (SELECT id FROM requisition.requisition_templates WHERE name = current_setting('vars.template'));
DELETE FROM siglusintegration.requisition_template_extension WHERE requisitiontemplateid IN (SELECT id FROM requisition.requisition_templates WHERE name = current_setting('vars.template'));
DELETE FROM requisition.columns_maps WHERE requisitiontemplateid IN (SELECT id FROM requisition.requisition_templates WHERE name = current_setting('vars.template'));
DELETE FROM requisition.requisition_template_assignments WHERE templateid IN (SELECT id FROM requisition.requisition_templates WHERE name = current_setting('vars.template'));
DELETE FROM requisition.requisition_templates WHERE name = current_setting('vars.template') AND id !=  current_setting('vars.templateid')::uuid;

-- update template
UPDATE requisition.requisition_templates SET modifieddate=now(),numberofperiodstoaverage=3,populatestockonhandfromstockcards=TRUE,archived=FALSE,name=current_setting('vars.template')
WHERE id = current_setting('vars.templateid')::uuid;

-- requisition_template_assignments
INSERT INTO requisition.requisition_template_assignments(id,programid,templateid,facilitytypeid)
    SELECT '2a23954a-22f1-11ef-9e35-acde48001122',current_setting('vars.programid')::uuid,current_setting('vars.templateid')::uuid,id
    FROM referencedata.facility_types ft
    WHERE ft.code IN ('HC');

-- requisition_template_extension
INSERT INTO siglusintegration.requisition_template_extension(id,requisitiontemplateid,enableconsultationnumber,enablekitusage,enableproduct,enablepatientlineitem,enableregimen,enablerapidtestconsumption,enableusageinformation,enablequicklyfill,enableagegroup) VALUES
('2a2395c2-22f1-11ef-9e35-acde48001122',current_setting('vars.templateid')::uuid,FALSE,FALSE,TRUE,TRUE,FALSE,FALSE,FALSE,TRUE,TRUE);

-- columns_maps
-- SOURCE (0: USER_INPUT, 1: CALCULATED , 2: REFERENCE_DATA, 3: STOCK_CARDS, 4: PREVIOUS_REQUISITION)
INSERT INTO requisition.columns_maps(displayorder,indicator,isdisplayed,key,label,name,source,definition,tag,requisitioncolumnoptionid,requisitioncolumnid,requisitiontemplateid) VALUES
(0,'S',TRUE,'skipped','Ignorar','skipped',4,'Select the check box below to skip a single product. Remove all data from the row prior to selection.',NULL,'17d6e860-a746-4500-a0fa-afc84d799dca','c6dffdee-3813-40d9-8737-f531d5adf420',current_setting('vars.templateid')::uuid),
(1,'R',TRUE,'orderable.fullProductName','Medicamento','orderable.fullProductName',2,'Product name',NULL,NULL,'e53e80de-fc63-4ecb-b6b2-ef376b34c926',current_setting('vars.templateid')::uuid),
(2,'U',TRUE,'orderable.dispensable.displayUnit','Unidade de Saída','orderable.dispensable.displayUnit',2,'Product unit',NULL,NULL,'61e6d059-10ef-40c4-a6e3-fa7b9ad741ec',current_setting('vars.templateid')::uuid),
(3,'A',TRUE,'beginningBalance','Stock Inicial','beginningBalance',0,'Initial stock quantity',NULL,NULL,'33b2d2e9-3167-46b0-95d4-1295be9afc22',current_setting('vars.templateid')::uuid),
(4,'B',TRUE,'totalReceivedQuantity','Entradas','totalReceivedQuantity',3,'Received quantity','received',NULL,'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',current_setting('vars.templateid')::uuid),
(6,'C',TRUE,'totalConsumedQuantity','Saidas','totalConsumedQuantity',0,'Issued quantity','consumed',NULL,'9e825396-269d-4873-baa4-89054e2722f4',current_setting('vars.templateid')::uuid),
(7,'D',TRUE,'totalLossesAndAdjustments','Perdas e Ajustes','totalLossesAndAdjustments',0,'Adjustment quantity','adjustment',NULL,'cd57f329-f549-4717-882e-ecbf98122c38',current_setting('vars.templateid')::uuid),
(8,'E',TRUE,'stockOnHand','Inventário','stockOnHand',0,'Current stock on hand',NULL,NULL,'752cda76-0db5-4b6e-bb79-0f531ab78e2c',current_setting('vars.templateid')::uuid),
(9,'EQ',FALSE ,'estimatedQuantity','Quantidade Estimada','estimatedQuantity',1,'Estimated Quantity',NULL,NULL,'878d9a4a-5be8-11ed-acc8-acde48001122',current_setting('vars.templateid')::uuid),
(10,'J',TRUE ,'requestedQuantity','Quantidade Sugerida','requestedQuantity',3,'Suggested quantity, calculated from regimens of client facilities',NULL,NULL,'4a2e9fd3-1127-4b68-9912-84a5c00f6999',current_setting('vars.templateid')::uuid),
(11,'SQ',TRUE ,'suggestedQuantity','Quantidade Pedida','suggestedQuantity',0,'Requested quantity',NULL,'4b83501e-18cf-44c4-80d4-2dea7f24c1b7','d06a04f9-1a81-45df-9921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(12,'K',TRUE,'approvedQuantity','Quantidade Aprovada','approvedQuantity',0,'Final approved quantity',NULL,NULL,'a62a5fed-c0b6-4d49-8a96-c631da0d0113',current_setting('vars.templateid')::uuid),
(13,'EX',TRUE,'expirationDate','Validade','expirationDate',1,'Date of the lot  which will be expired first since today',NULL,NULL,'d36a84f9-1a81-45df-1921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(100,'Z',FALSE,'additionalQuantityRequired','Additional quantity required','additionalQuantityRequired',0,'Additional quantity required for new patients',NULL,NULL,'90a84767-1009-4d8e-be13-f006fce2a002',current_setting('vars.templateid')::uuid),
(100,'N',FALSE,'adjustedConsumption','Adjusted consumption','adjustedConsumption',1,'Total consumed quantity after adjusting for stockout days. Quantified in dispensing units.',NULL,NULL,'720dd95b-b765-4afb-b7f2-7b22261c32f3',current_setting('vars.templateid')::uuid),
(100,'QA',FALSE,'authorizedQuantity','Quantidade Autorizada','authorizedQuantity',0,'Final authorized quantity',NULL,NULL,'03d4fe58-12a1-41ce-80b8-d6374342d7ae',current_setting('vars.templateid')::uuid),
(100,'P',FALSE,'averageConsumption','Consumo Médio','averageConsumption',3,'Average consumption over a specified number of periods/months. Quantified in dispensing units.',NULL,NULL,'89113ec3-40e9-4d81-9516-b56adba7f8cd',current_setting('vars.templateid')::uuid),
(100,'I',FALSE,'calculatedOrderQuantity','Calculated order quantity','calculatedOrderQuantity',1,'Actual quantity needed after deducting stock in hand. This is quantified in dispensing units.',NULL,NULL,'5528576b-b1e7-48d9-bf32-fd0eefefaa9a',current_setting('vars.templateid')::uuid),
(100,'S',FALSE,'calculatedOrderQuantityIsa','Calc Order Qty ISA','calculatedOrderQuantityIsa',1,'Calculated Order Quantity ISA is based on an ISA configured by commodity type, and several trade items may fill for one commodity type.',NULL,NULL,'9ac91518-38c7-494c-95d9-c37ef2ffff81',current_setting('vars.templateid')::uuid),
(100,'DI',FALSE,'difference','Diferenças','difference',1,'Difference = Inventory -(Stock at Beginning of Period + Sum of Entries - Issues)',NULL,NULL,'96bd0839-4a83-4588-8d78-5566ef80dc89',current_setting('vars.templateid')::uuid),
(100,'G',FALSE,'idealStockAmount','Ideal Stock Amount','idealStockAmount',2,'The Ideal Stock Amount is the target quantity for a specific commodity type, facility, and period.',NULL,NULL,'aa0a1a7e-e5cb-4385-b781-943316fa116a',current_setting('vars.templateid')::uuid),
(100,'H',FALSE,'maximumStockQuantity','Maximum stock quantity','maximumStockQuantity',1,'Maximum stock calculated based on consumption and max stock amounts. Quantified in dispensing units.',NULL,'ff2b350c-37f2-4801-b21e-27ca12c12b3c','913e1a4f-f3b0-40c6-a422-2f73608c6f3d',current_setting('vars.templateid')::uuid),
(100,'F',FALSE,'numberOfNewPatientsAdded','Number of new patients added','numberOfNewPatientsAdded',0,'New patients data.',NULL,'4957ebb4-297c-459e-a291-812e72286eff','5708ebf9-9317-4420-85aa-71b2ae92643d',current_setting('vars.templateid')::uuid),
(100,'O',FALSE,'orderable.productCode','Código do Produto','orderable.productCode',2,'Product code',NULL,NULL,'bde01507-3837-47b7-ae08-cec92c0c3cd2',current_setting('vars.templateid')::uuid),
(100,'V',FALSE,'packsToShip','Packs to ship','packsToShip',1,'Total packs to be shipped based on pack size and applying rounding rules.',NULL,'dcf41f06-3000-4af6-acf5-5de4fffc966f','dc9dde56-593d-4929-81be-d1faec7025a8',current_setting('vars.templateid')::uuid),
(100,'T',FALSE,'pricePerPack','Price per pack','pricePerPack',2,'Price per pack. Will be blank if price is not defined.',NULL,NULL,'df524868-9d0a-18e6-80f5-76304ded7ab9',current_setting('vars.templateid')::uuid),
(100,'L',FALSE,'remarks','Remarks','remarks',0,'Any additional remarks.',NULL,NULL,'2ed8c74a-f424-4742-bd14-cfbe67b6e7be',current_setting('vars.templateid')::uuid),
(100,'W',FALSE,'requestedQuantityExplanation','Requested quantity explanation','requestedQuantityExplanation',0,'Explanation of request for a quantity other than calculated order quantity.',NULL,NULL,'6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f5',current_setting('vars.templateid')::uuid),
(100,'TQ',FALSE,'theoreticalQuantityToRequest','Quantidade Teorica a Requisitar','theoreticalQuantityToRequest',1,'Theoretical Quantity to Request = 2 * issues - inventory',NULL,NULL,'d546e757-5276-4806-87fe-aa10017467d5',current_setting('vars.templateid')::uuid),
(100,'TS',FALSE,'theoreticalStockAtEndofPeriod','Stock Teorico no Final do Periodo','theoreticalStockAtEndofPeriod',1,'Theoretical Stock at End of Period = stock at beginning of period + sum of entries -issues',NULL,NULL,'7e022648-52da-46ab-8001-a0e6388a0964',current_setting('vars.templateid')::uuid),
(100,'Y',FALSE,'total','Total','total',1,'Total of beginning balance and quantity received.',NULL,NULL,'ef524868-9d0a-11e6-80f5-76304dec7eb7',current_setting('vars.templateid')::uuid),
(100,'Q',FALSE,'totalCost','Total cost','totalCost',1,'Total cost of the product based on quantity requested. Will be blank if price is not defined.',NULL,NULL,'e3a0c1fc-c2d5-11e6-af2d-3417eb83144e',current_setting('vars.templateid')::uuid),
(100,'X',FALSE,'totalStockoutDays','Total dias em rutura','totalStockoutDays',3,'Total number of days facility was out of stock.',NULL,NULL,'750b9359-c097-4612-8328-d21671f88920',current_setting('vars.templateid')::uuid);

-- usage_sections_maps
INSERT INTO siglusintegration.usage_sections_maps(id,category,displayorder,label,name,sectionid,requisitiontemplateid) VALUES
('e5bb3402-708c-11ed-a4c1-acde48001122','AGEGROUP',0,'Faixas Etarias','group','43fa95ae-fcfb-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('e5bb343e-708c-11ed-a4c1-acde48001122','AGEGROUP',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb3484-708c-11ed-a4c1-acde48001122','CONSULTATIONNUMBER',0,'Consultation Number','number','bcd979a4-1967-4258-a5be-b04f82c4c62c',current_setting('vars.templateid')::uuid),
('e5bb34c0-708c-11ed-a4c1-acde48001122','KITUSAGE',0,'KIT data collection','collection','bce979a4-1957-4259-a5bb-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('e5bb34fc-708c-11ed-a4c1-acde48001122','KITUSAGE',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb3542-708c-11ed-a4c1-acde48001122','PATIENT',0,'Fases de Tratamento (Adulto)','newSection3',NULL,current_setting('vars.templateid')::uuid),
('e5bb357e-708c-11ed-a4c1-acde48001122','PATIENT',1,'Fases de Tratamento (Pediatrico)','newSection4',NULL,current_setting('vars.templateid')::uuid),
('e5bb35c4-708c-11ed-a4c1-acde48001122','PATIENT',2,'PUs e Farmácia Ambulatório','newSection2',NULL,current_setting('vars.templateid')::uuid),
('e5bb3600-708c-11ed-a4c1-acde48001122','PATIENT',3,'Pacientes Novos no Sector da TB','patientType','bbf232b4-4b07-4a8e-a22c-1607995b01cd',current_setting('vars.templateid')::uuid),
('e5bb3646-708c-11ed-a4c1-acde48001122','PATIENT',4,'Seguimento Profilaxias (Pus e Farmácia Pública)','newSection0',NULL,current_setting('vars.templateid')::uuid),
('e5bb3682-708c-11ed-a4c1-acde48001122','PATIENT',5,'Tipo de Dispensa dos Profilacticos','newSection1',NULL,current_setting('vars.templateid')::uuid),
('e5bb36c8-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',0,'Test Project','project','97230a23-f47b-4115-86fc-f91760b7b439',current_setting('vars.templateid')::uuid),
('e5bb3704-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',1,'Test Outcome','outcome','0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',current_setting('vars.templateid')::uuid),
('e5bb3740-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',2,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb3786-708c-11ed-a4c1-acde48001122','REGIMEN',0,'Regimen','regimen','91deacfc-1f06-4052-a7d2-0c78a1642043',current_setting('vars.templateid')::uuid),
('e5bbb8a0-708c-11ed-a4c1-acde48001122','REGIMEN',1,'Summary','summary','e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',current_setting('vars.templateid')::uuid),
('e5bbb9fe-708c-11ed-a4c1-acde48001122','USAGEINFORMATION',0,'Product Usage Information','information','bce979a4-1876-4259-9102-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('e5bbba6c-708c-11ed-a4c1-acde48001122','USAGEINFORMATION',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid);

-- usage_columns_maps
INSERT INTO siglusintegration.usage_columns_maps(id,usagesectionid,displayorder,indicator,isdisplayed,label,name,definition,source,tag,availablesources,usagecolumnid,requisitiontemplateid) VALUES
('2a239662-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Sensível Intensiva','new','Sensível Intensiva','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a2396ee-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Sensível Manutenção','newColumn6','Sensível Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a239748-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'MR Indução','newColumn0','MR Indução','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2397ac-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'MR Intensiva','newColumn1','MR Intensiva','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a239806-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'MR Manutenção','newColumn2','MR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a239860-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'XR Manutenção','newColumn3','XR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2398c4-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',6,'N',TRUE,'XR Intensiva','newColumn4','XR Intensiva','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23991e-22f1-11ef-9e35-acde48001122','e5bb357e-708c-11ed-a4c1-acde48001122',7,'PD',FALSE,'Total','total','calculate total','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a239978-22f1-11ef-9e35-acde48001122','e5bbb8a0-708c-11ed-a4c1-acde48001122',0,'SU',TRUE,'1st linhas','1stLinhas','display on the second part of the regimen section as the first lines','USER_INPUT',NULL,'USER_INPUT','73a20c66-c0f5-45d3-8268-336198296e33',current_setting('vars.templateid')::uuid),
('2a2399d2-22f1-11ef-9e35-acde48001122','e5bbb8a0-708c-11ed-a4c1-acde48001122',1,'SU',TRUE,'Total','total','Count the total number in the second part of the regimen section','CALCULATED',NULL,'CALCULATED|USER_INPUT','676665ea-ba70-4742-b4d3-c512e7a9f389',current_setting('vars.templateid')::uuid),
('2a239a2c-22f1-11ef-9e35-acde48001122','e5bb343e-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'Adultos','adultos','Adults','USER_INPUT',NULL,'USER_INPUT','13263196-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a239bee-22f1-11ef-9e35-acde48001122','e5bb343e-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Criança < 25Kg','criança < 25Kg','Child < 25 kg','USER_INPUT',NULL,'USER_INPUT','1326342a-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a239ca2-22f1-11ef-9e35-acde48001122','e5bb343e-708c-11ed-a4c1-acde48001122',2,'SV',TRUE,'Criança > 25Kg','criança > 25Kg','Child > 25 kg','USER_INPUT',NULL,'USER_INPUT','c1092b36-fd01-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a239d4c-22f1-11ef-9e35-acde48001122','e5bb34c0-708c-11ed-a4c1-acde48001122',0,'KD',TRUE,'No. de Kits Recebidos','kitReceived','record the quantity of how many KIT received','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','23c0ecc1-f58e-41e4-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('2a239dec-22f1-11ef-9e35-acde48001122','e5bb34c0-708c-11ed-a4c1-acde48001122',1,'KD',TRUE,'Nº de Kits Abertos e Enviados','kitOpened','record the quantity of how many KIT opened and issued','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','86ca8cea-94c2-4d50-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid),
('2a239e96-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Isoniazida 100 mg','new','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a239f36-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',1,'PD',FALSE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a239fd6-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Isoniazida 300 mg','newColumn0','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a080-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Levofloxacina 100 mg Disp','newColumn1','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a120-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'Levofloxacina 250 mg Rev','newColumn2','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a198-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'Rifapentina 300 mg + Isoniazida 300 mg','newColumn3','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a1f2-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',6,'N',TRUE,'Rifapentina 150 mg','newColumn4','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a256-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',7,'N',TRUE,'Piridoxina 25mg','newColumn5','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a2b0-22f1-11ef-9e35-acde48001122','e5bb35c4-708c-11ed-a4c1-acde48001122',8,'N',TRUE,'Piridoxina 50mg','newColumn6','record number of tablets for the regimen product','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a30a-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Novo Adulto Sensivel','new','Novo Adulto Sensivel','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a23a364-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Novo Adulto MR','newColumn0','Novo Adulto MR','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a3c8-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Novo Adulto XR','newColumn1','Novo Adulto XR','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a422-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Novo Criança Sensivel','newColumn2','Novo Criança Sensivel','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a47c-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'Novo Criança MR','newColumn3','Novo Criança MR','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a4d6-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'Novo Criança XR','newColumn4','Novo Criança XR','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a530-22f1-11ef-9e35-acde48001122','e5bb3600-708c-11ed-a4c1-acde48001122',6,'PD',TRUE,'Total','total','record the total number of this group','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a23a594-22f1-11ef-9e35-acde48001122','e5bb3484-708c-11ed-a4c1-acde48001122',0,'CN',TRUE,'Nº de Consultas Externas Realizadas','consultationNumber','record the number of consultations performed in this period','USER_INPUT',NULL,'USER_INPUT','23c0edc1-9382-7161-99f2-241a3e8360c6',current_setting('vars.templateid')::uuid),
('2a23a5ee-22f1-11ef-9e35-acde48001122','e5bb3484-708c-11ed-a4c1-acde48001122',1,'CN',FALSE,'Total','total','record the total number','USER_INPUT',NULL,'USER_INPUT|CALCULATED','95327492-2874-3836-8fa0-dd2b5ced3e8c',current_setting('vars.templateid')::uuid),
('2a23a648-22f1-11ef-9e35-acde48001122','e5bb3646-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Início','new','Início','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a23a6a2-22f1-11ef-9e35-acde48001122','e5bb3646-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Contínua/Manutenção','newColumn0','Contínua/Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a706-22f1-11ef-9e35-acde48001122','e5bb3646-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Final/última Dispensa','newColumn1','Final/última Dispensa','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23a760-22f1-11ef-9e35-acde48001122','e5bb3646-708c-11ed-a4c1-acde48001122',3,'PD',TRUE,'Total','total','record the total number of this group','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a23a7ba-22f1-11ef-9e35-acde48001122','e5bb3786-708c-11ed-a4c1-acde48001122',0,'RE',TRUE,'Código','code','display the code of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','c47396fe-381a-49a2-887e-ce25c80b0875',current_setting('vars.templateid')::uuid),
('2a23a814-22f1-11ef-9e35-acde48001122','e5bb3786-708c-11ed-a4c1-acde48001122',1,'RE',TRUE,'Regimes Terapeuticos','regiment','display the name of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','55353f84-d5a1-4a60-985e-ec3c04212575',current_setting('vars.templateid')::uuid),
('2a23a86e-22f1-11ef-9e35-acde48001122','e5bb3786-708c-11ed-a4c1-acde48001122',2,'RE',TRUE,'Total doente','patients','record the number of patients','USER_INPUT',NULL,'USER_INPUT','1a44bc23-b652-4a72-b74b-98210d44101c',current_setting('vars.templateid')::uuid),
('2a23a8d2-22f1-11ef-9e35-acde48001122','e5bb3786-708c-11ed-a4c1-acde48001122',3,'RE',TRUE,'Farmácia Comunitária','community','record the number of patients in community pharmacy','USER_INPUT',NULL,'USER_INPUT','04065445-3aaf-4928-b329-8454964b62f8',current_setting('vars.templateid')::uuid),
('2a23a92c-22f1-11ef-9e35-acde48001122','e5bb36c8-708c-11ed-a4c1-acde48001122',0,'TP',TRUE,'Fases de Tratamento','hivDetermine','Fases de Tratamento',NULL,NULL,E'','28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',current_setting('vars.templateid')::uuid),
('2a23a986-22f1-11ef-9e35-acde48001122','e5bbb9fe-708c-11ed-a4c1-acde48001122',0,'PU',TRUE,'N Tratamentos atendidos neste mês','treatmentsAttended','record the quantity of patients for each treatment by products','USER_INPUT',NULL,'USER_INPUT','23c0ecc1-9182-7161-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('2a23a9ea-22f1-11ef-9e35-acde48001122','e5bbb9fe-708c-11ed-a4c1-acde48001122',1,'PU',TRUE,'Stock Existente no Final do Período','existentStock','record the SOH of the product','USER_INPUT',NULL,'USER_INPUT','86ca8cea-9281-9281-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid),
('2a23aa44-22f1-11ef-9e35-acde48001122','e5bb3740-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'Adulto','HF','Adulto','USER_INPUT',NULL,'USER_INPUT','c280a232-a39e-4ea9-850b-7bb9fcc2d848',current_setting('vars.templateid')::uuid),
('2a23aaa8-22f1-11ef-9e35-acde48001122','e5bb3740-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Pediatrico','total','Pediatrico','USER_INPUT',NULL,'USER_INPUT|CALCULATED','09e5d451-0ffe-43df-ae00-2f15f2a3681b',current_setting('vars.templateid')::uuid),
('2a23ab02-22f1-11ef-9e35-acde48001122','e5bb3740-708c-11ed-a4c1-acde48001122',2,'SV',FALSE,'APES','APES','record the related test outcomes for APES','USER_INPUT',NULL,'USER_INPUT','379692a8-12f4-4c35-868a-9b6055c8fa8e',current_setting('vars.templateid')::uuid),
('2a23ab5c-22f1-11ef-9e35-acde48001122','e5bb34fc-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'HF','HF','record the quantity of KIT data in my facility',NULL,NULL,E'','cbee99e4-f100-4f9e-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('2a23abc0-22f1-11ef-9e35-acde48001122','e5bb34fc-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'CHW','CHW','record the quantity of KIT data in CHW','USER_INPUT',NULL,'USER_INPUT','95227492-c394-4f7e-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
('2a23ac1a-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Sensível Intensiva','new','Sensível Intensiva','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a23ac74-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Sensível Manutenção','newColumn6','Sensível Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23acd8-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'MR Indução','newColumn0','MR Indução','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23ad32-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'MR Intensiva','newColumn1','MR Intensiva','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23ad8c-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'MR Manutenção','newColumn2','MR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23ade6-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'XR Indução','newColumn3','XR Indução','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23ae40-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',6,'N',TRUE,'XR Manutenção','newColumn4','XR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23ae9a-22f1-11ef-9e35-acde48001122','e5bb3542-708c-11ed-a4c1-acde48001122',7,'PD',FALSE,'Total','total','calculate total','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a23aef4-22f1-11ef-9e35-acde48001122','e5bb3682-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Mensal','new','Mensal','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a23af4e-22f1-11ef-9e35-acde48001122','e5bb3682-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Trimenstral','newColumn0','Trimenstral','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23afb2-22f1-11ef-9e35-acde48001122','e5bb3682-708c-11ed-a4c1-acde48001122',3,'PD',TRUE,'Total','total','record the total number of this group','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a23b00c-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',0,'TO',TRUE,'Sensível Intensiva','consumo','Sensível Intensiva','USER_INPUT',NULL,'USER_INPUT','743a0575-6d00-4ff0-89a6-1a76de1c1714',current_setting('vars.templateid')::uuid),
('2a23b066-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',1,'TO',FALSE,'not use1','positive','not use1','USER_INPUT',NULL,'USER_INPUT','becae41f-1436-4f67-87ac-dece0b97d417',current_setting('vars.templateid')::uuid),
('2a23b0c0-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',2,'TO',FALSE,'not use2','unjustified','not use2','USER_INPUT',NULL,'USER_INPUT','fe6e0f40-f47b-41e2-be57-8064876d75f6',current_setting('vars.templateid')::uuid),
('2a23b11a-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Sensível Manutenção','newColumn5','Sensível Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b174-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'MR Indução','newColumn6','MR Indução','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b1d8-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'MR Intensiva','newColumn4','MR Intensiva','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b232-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',6,'N',TRUE,'MR Manutenção','newColumn0','MR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b28c-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',7,'N',TRUE,'XR Indução','newColumn1','XR Indução','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b2e6-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',8,'N',TRUE,'XR Intensiva','newColumn3','XR Intensiva','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b552-22f1-11ef-9e35-acde48001122','e5bb3704-708c-11ed-a4c1-acde48001122',9,'N',TRUE,'XR Manutenção','newColumn2','XR Manutenção','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23b610-22f1-11ef-9e35-acde48001122','e5bbba6c-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'HF','HF','record the product usage information for my facility','USER_INPUT',NULL,'USER_INPUT','cbee99e4-1827-0291-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('2a23b6ba-22f1-11ef-9e35-acde48001122','e5bbba6c-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Total','total','record the total number of each column','USER_INPUT',NULL,'USER_INPUT|CALCULATED','95227492-2874-2836-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
('2a23b764-22f1-11ef-9e35-acde48001122','e5bb3402-708c-11ed-a4c1-acde48001122',0,'PU',TRUE,'Tratamento','treatment','record the quantity of patients for each age group in treatment','USER_INPUT',NULL,'USER_INPUT','9a384fee-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a23b804-22f1-11ef-9e35-acde48001122','e5bb3402-708c-11ed-a4c1-acde48001122',1,'PU',TRUE,'Profilaxia','prophylaxis','record the quantity of patients for each age group in prophylaxis','USER_INPUT',NULL,'USER_INPUT','a4d2a594-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid);
