-- MMIA Template - HC
SET local vars.template = 'MMIA Template - HC';
SET local vars.templateid = '5dcd95fa-971d-49e4-98e7-cebfb6f11c56';
SET local vars.programid = '10845cb9-d365-4aaa-badd-b4fa39c6a26a';

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
SELECT '2a22f9a0-22f1-11ef-9e35-acde48001122',current_setting('vars.programid')::uuid,current_setting('vars.templateid')::uuid,id
FROM referencedata.facility_types ft
WHERE ft.code IN ('HC');

-- requisition_template_extension
INSERT INTO siglusintegration.requisition_template_extension(id,requisitiontemplateid,enableconsultationnumber,enablekitusage,enableproduct,enablepatientlineitem,enableregimen,enablerapidtestconsumption,enableusageinformation,enablequicklyfill,enableagegroup) VALUES
    ('2a22f9fa-22f1-11ef-9e35-acde48001122',current_setting('vars.templateid')::uuid,FALSE,FALSE,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,FALSE);

-- columns_maps
-- SOURCE (0: USER_INPUT, 1: CALCULATED , 2: REFERENCE_DATA, 3: STOCK_CARDS, 4: PREVIOUS_REQUISITION)
INSERT INTO requisition.columns_maps(displayorder,indicator,isdisplayed,key,label,name,source,definition,tag,requisitioncolumnoptionid,requisitioncolumnid,requisitiontemplateid) VALUES
(0,'S',TRUE,'skipped','Ignorar','skipped',0,'Select the check box below to skip a single product. Remove all data from the row prior to selection.',NULL,'17d6e860-a746-4500-a0fa-afc84d799dca','c6dffdee-3813-40d9-8737-f531d5adf420',current_setting('vars.templateid')::uuid),
(1,'O',TRUE,'orderable.productCode','FNM','orderable.productCode',2,'Product code',NULL,NULL,'bde01507-3837-47b7-ae08-cec92c0c3cd2',current_setting('vars.templateid')::uuid),
(2,'R',TRUE,'orderable.fullProductName','MEDICAMENTO','orderable.fullProductName',2,'Product name',NULL,NULL,'e53e80de-fc63-4ecb-b6b2-ef376b34c926',current_setting('vars.templateid')::uuid),
(3,'U',TRUE,'orderable.dispensable.displayUnit','Unidade','orderable.dispensable.displayUnit',2,'Product unit',NULL,NULL,'61e6d059-10ef-40c4-a6e3-fa7b9ad741ec',current_setting('vars.templateid')::uuid),
(4,'A',TRUE,'beginningBalance','Stock Inicial','beginningBalance',0,'Initial stock quantity',NULL,NULL,'33b2d2e9-3167-46b0-95d4-1295be9afc22',current_setting('vars.templateid')::uuid),
(5,'B',TRUE,'totalReceivedQuantity','Entradas','totalReceivedQuantity',3,'Received quantity','received',NULL,'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',current_setting('vars.templateid')::uuid),
(6,'C',TRUE,'totalConsumedQuantity','Saídas','totalConsumedQuantity',0,'Issued quantity','consumed',NULL,'9e825396-269d-4873-baa4-89054e2722f4',current_setting('vars.templateid')::uuid),
(7,'D',TRUE,'totalLossesAndAdjustments','Perdas e Ajustes','totalLossesAndAdjustments',0,'Adjustment quantity','adjustment',NULL,'cd57f329-f549-4717-882e-ecbf98122c38',current_setting('vars.templateid')::uuid),
(8,'E',TRUE,'stockOnHand','Inventário','stockOnHand',0,'Current stock on hand',NULL,NULL,'752cda76-0db5-4b6e-bb79-0f531ab78e2c',current_setting('vars.templateid')::uuid),
(9,'EQ',FALSE,'estimatedQuantity','Quantidade Estimada','estimatedQuantity',1,'Estimated Quantity',NULL,NULL,'878d9a4a-5be8-11ed-acc8-acde48001122',current_setting('vars.templateid')::uuid),
(10,'J',TRUE,'requestedQuantity','Quantidade Sugerida','requestedQuantity',3,'Suggested quantity, calculated from regimens of client facilities',NULL,NULL,'4a2e9fd3-1127-4b68-9912-84a5c00f6999',current_setting('vars.templateid')::uuid),
(11,'SQ',TRUE,'suggestedQuantity','Quantidade Pedida','suggestedQuantity',0,'Requested quantity',NULL,'4b83501e-18cf-44c4-80d4-2dea7f24c1b7','d06a04f9-1a81-45df-9921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(12,'K',TRUE,'approvedQuantity','Quantidade Aprovada','approvedQuantity',0,'Final approved quantity',NULL,NULL,'a62a5fed-c0b6-4d49-8a96-c631da0d0113',current_setting('vars.templateid')::uuid),
(13,'EX',TRUE,'expirationDate','Validade','expirationDate',1,'Date of the lot  which will be expired first since today',NULL,NULL,'d36a84f9-1a81-45df-1921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(100,'Z',FALSE,'additionalQuantityRequired','Additional quantity required','additionalQuantityRequired',0,'Additional quantity required for new patients',NULL,NULL,'90a84767-1009-4d8e-be13-f006fce2a002',current_setting('vars.templateid')::uuid),
(100,'N',FALSE,'adjustedConsumption','Adjusted consumption','adjustedConsumption',1,'Total consumed quantity after adjusting for stockout days. Quantified in dispensing units.',NULL,NULL,'720dd95b-b765-4afb-b7f2-7b22261c32f3',current_setting('vars.templateid')::uuid),
(100,'QA',FALSE,'authorizedQuantity','Quantity Authorized','authorizedQuantity',0,'Final authorized quantity',NULL,NULL,'03d4fe58-12a1-41ce-80b8-d6374342d7ae',current_setting('vars.templateid')::uuid),
(100,'P',FALSE,'averageConsumption','Average consumption','averageConsumption',3,'Average consumption over a specified number of periods/months. Quantified in dispensing units.',NULL,NULL,'89113ec3-40e9-4d81-9516-b56adba7f8cd',current_setting('vars.templateid')::uuid),
(100,'I',FALSE,'calculatedOrderQuantity','Quantidade do pedido','calculatedOrderQuantity',1,'Actual quantity needed after deducting stock in hand. This is quantified in dispensing units.',NULL,NULL,'5528576b-b1e7-48d9-bf32-fd0eefefaa9a',current_setting('vars.templateid')::uuid),
(100,'S',FALSE,'calculatedOrderQuantityIsa','Calc Order Qty ISA','calculatedOrderQuantityIsa',1,'Calculated Order Quantity ISA is based on an ISA configured by commodity type, and several trade items may fill for one commodity type.',NULL,NULL,'9ac91518-38c7-494c-95d9-c37ef2ffff81',current_setting('vars.templateid')::uuid),
(100,'DI',FALSE,'difference','Diferença','difference',1,'Difference=Inventory -(Stock at Beginning of Period + Sum of Entries - Issues).',NULL,NULL,'96bd0839-4a83-4588-8d78-5566ef80dc89',current_setting('vars.templateid')::uuid),
(100,'G',FALSE,'idealStockAmount','Ideal Stock Amount','idealStockAmount',2,'The Ideal Stock Amount is the target quantity for a specific commodity type, facility, and period.',NULL,NULL,'aa0a1a7e-e5cb-4385-b781-943316fa116a',current_setting('vars.templateid')::uuid),
(100,'H',FALSE,'maximumStockQuantity','Maximum stock quantity','maximumStockQuantity',1,'Maximum stock calculated based on consumption and max stock amounts. Quantified in dispensing units.',NULL,'ff2b350c-37f2-4801-b21e-27ca12c12b3c','913e1a4f-f3b0-40c6-a422-2f73608c6f3d',current_setting('vars.templateid')::uuid),
(100,'F',FALSE,'numberOfNewPatientsAdded','Number of new patients added','numberOfNewPatientsAdded',0,'New patients data.',NULL,'4957ebb4-297c-459e-a291-812e72286eff','5708ebf9-9317-4420-85aa-71b2ae92643d',current_setting('vars.templateid')::uuid),
(100,'V',FALSE,'packsToShip','Packs to ship','packsToShip',1,'Total packs to be shipped based on pack size and applying rounding rules.',NULL,'dcf41f06-3000-4af6-acf5-5de4fffc966f','dc9dde56-593d-4929-81be-d1faec7025a8',current_setting('vars.templateid')::uuid),
(100,'T',FALSE,'pricePerPack','Price per pack','pricePerPack',2,'Price per pack. Will be blank if price is not defined.',NULL,NULL,'df524868-9d0a-18e6-80f5-76304ded7ab9',current_setting('vars.templateid')::uuid),
(100,'L',FALSE,'remarks','Remarks','remarks',0,'Any additional remarks.',NULL,NULL,'2ed8c74a-f424-4742-bd14-cfbe67b6e7be',current_setting('vars.templateid')::uuid),
(100,'W',FALSE,'requestedQuantityExplanation','Requested quantity explanation','requestedQuantityExplanation',0,'Explanation of request for a quantity other than calculated order quantity.',NULL,NULL,'6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f5',current_setting('vars.templateid')::uuid),
(100,'TQ',FALSE,'theoreticalQuantityToRequest','Theoretical Quantity to Request','theoreticalQuantityToRequest',1,'Theoretical Quantity to Request=2 * issues - inventory',NULL,NULL,'d546e757-5276-4806-87fe-aa10017467d5',current_setting('vars.templateid')::uuid),
(100,'TS',FALSE,'theoreticalStockAtEndofPeriod','Stock Teórico no Final do Periodo','theoreticalStockAtEndofPeriod',1,'Theoretical Stock at End of Period=stock at beginning of period + sum of entries -issues',NULL,NULL,'7e022648-52da-46ab-8001-a0e6388a0964',current_setting('vars.templateid')::uuid),
(100,'Y',FALSE,'total','Total','total',1,'Total of beginning balance and quantity received.',NULL,NULL,'ef524868-9d0a-11e6-80f5-76304dec7eb7',current_setting('vars.templateid')::uuid),
(100,'Q',FALSE,'totalCost','Total cost','totalCost',1,'Total cost of the product based on quantity requested. Will be blank if price is not defined.',NULL,NULL,'e3a0c1fc-c2d5-11e6-af2d-3417eb83144e',current_setting('vars.templateid')::uuid),
(100,'X',FALSE,'totalStockoutDays','Total stockout days','totalStockoutDays',3,'Total number of days facility was out of stock.',NULL,NULL,'750b9359-c097-4612-8328-d21671f88920',current_setting('vars.templateid')::uuid);

-- usage_sections_maps
INSERT INTO siglusintegration.usage_sections_maps(id,category,displayorder,label,name,sectionid,requisitiontemplateid) VALUES
('e5bb267e-708c-11ed-a4c1-acde48001122','AGEGROUP',0,'Faixas Etarias','group','43fa95ae-fcfb-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('e5bb26ba-708c-11ed-a4c1-acde48001122','AGEGROUP',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb2700-708c-11ed-a4c1-acde48001122','CONSULTATIONNUMBER',0,'Consultation Number','number','bcd979a4-1967-4258-a5be-b04f82c4c62c',current_setting('vars.templateid')::uuid),
('e5bb273c-708c-11ed-a4c1-acde48001122','KITUSAGE',0,'KIT data collection','collection','bce979a4-1957-4259-a5bb-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('e5bb2782-708c-11ed-a4c1-acde48001122','KITUSAGE',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb27be-708c-11ed-a4c1-acde48001122','PATIENT',0,'Tipo de doentes em TARV','patientType','bbf232b4-4b07-4a8e-a22c-1607995b01cd',current_setting('vars.templateid')::uuid),
('e5bb27fa-708c-11ed-a4c1-acde48001122','PATIENT',1,'Faixa Etária dos Pacientes TARV','newSection0',NULL,current_setting('vars.templateid')::uuid),
('e5bb2840-708c-11ed-a4c1-acde48001122','PATIENT',2,'Profilaxia','newSection1',NULL,current_setting('vars.templateid')::uuid),
('e5bb287c-708c-11ed-a4c1-acde48001122','PATIENT',3,'Total global','newSection8',NULL,current_setting('vars.templateid')::uuid),
('e5bb28c2-708c-11ed-a4c1-acde48001122','PATIENT',4,'Tipo de Dispensa - Dispensa para 6 Meses (DS)','newSection2',NULL,current_setting('vars.templateid')::uuid),
('e5bb28fe-708c-11ed-a4c1-acde48001122','PATIENT',5,'Tipo de Dispensa - Dispensa para 3 Meses (DT)','newSection3',NULL,current_setting('vars.templateid')::uuid),
('e5bb2900-708c-11ed-a4c1-acde48001122','PATIENT',6,'Tipo de Dispensa - Dispensa Bi-Mestral (DB)','newSection9',NULL,current_setting('vars.templateid')::uuid),
('e5bb2944-708c-11ed-a4c1-acde48001122','PATIENT',7,'Tipo de Dispensa - Dispensa Mensal(DM)','newSection4',NULL,current_setting('vars.templateid')::uuid),
('e5bb2980-708c-11ed-a4c1-acde48001122','PATIENT',8,'Tipo de Dispensa - Levantaram no mês','newSection5',NULL,current_setting('vars.templateid')::uuid),
('e5bb29bc-708c-11ed-a4c1-acde48001122','PATIENT',9,'Tipo de Dispensa - Total de pacientes com tratamento','newSection6',NULL,current_setting('vars.templateid')::uuid),
('e5bb2a02-708c-11ed-a4c1-acde48001122','PATIENT',10,'Tipo de Dispensa - Ajuste','newSection7',NULL,current_setting('vars.templateid')::uuid),
('e5bb2a3e-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',0,'Test Project','project','97230a23-f47b-4115-86fc-f91760b7b439',current_setting('vars.templateid')::uuid),
('e5bb2a84-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',1,'Test Outcome','outcome','0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',current_setting('vars.templateid')::uuid),
('e5bb2ac0-708c-11ed-a4c1-acde48001122','RAPIDTESTCONSUMPTION',2,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('e5bb2b06-708c-11ed-a4c1-acde48001122','REGIMEN',0,'Regimen','regimen','91deacfc-1f06-4052-a7d2-0c78a1642043',current_setting('vars.templateid')::uuid),
('e5bb2b56-708c-11ed-a4c1-acde48001122','REGIMEN',1,'Linhas terapêuticas','summary','e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',current_setting('vars.templateid')::uuid),
('e5bb2b9c-708c-11ed-a4c1-acde48001122','USAGEINFORMATION',0,'Product Usage Information','information','bce979a4-1876-4259-9102-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('e5bb30b0-708c-11ed-a4c1-acde48001122','USAGEINFORMATION',1,'Services','service','bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid);

-- usage_columns_maps
INSERT INTO siglusintegration.usage_columns_maps(id,usagesectionid,displayorder,indicator,isdisplayed,label,name,definition,source,tag,availablesources,usagecolumnid,requisitiontemplateid) VALUES
('2a22fa54-22f1-11ef-9e35-acde48001122','e5bb26ba-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'Adultos','adultos','Adults','USER_INPUT',NULL,'USER_INPUT','13263196-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a22faae-22f1-11ef-9e35-acde48001122','e5bb26ba-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Criança < 25Kg','criança < 25Kg','Child < 25 kg','USER_INPUT',NULL,'USER_INPUT','1326342a-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a22fb12-22f1-11ef-9e35-acde48001122','e5bb26ba-708c-11ed-a4c1-acde48001122',2,'SV',TRUE,'Criança > 25Kg','criança > 25Kg','Child > 25 kg','USER_INPUT',NULL,'USER_INPUT','c1092b36-fd01-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a22fb6c-22f1-11ef-9e35-acde48001122','e5bb27fa-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Adultos','new','Age range for patients in ARV treatment','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a22fbc6-22f1-11ef-9e35-acde48001122','e5bb27fa-708c-11ed-a4c1-acde48001122',1,'PD',FALSE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a22fc20-22f1-11ef-9e35-acde48001122','e5bb27fa-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Pediátricos 0 aos 4 anos','newColumn0','Pediatric from 0 to 4 years old','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22fc7a-22f1-11ef-9e35-acde48001122','e5bb27fa-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Pediátricos 5 aos 9 anos','newColumn1','Pediatric from 5 to 9 years old','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22fcde-22f1-11ef-9e35-acde48001122','e5bb27fa-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'Pediátricos 10 aos 14 anos','newColumn2','Pediatric from 10 to 14 years old','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22fd38-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'5 meses atrás','new','5 months ago','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a22fd92-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'4 meses atrás','newColumn0','4 months ago','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22fdf6-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'3 meses atrás','newColumn1','3 months ago','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22fe50-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'2 meses atrás','newColumn2','2 months ago','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22feaa-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'Mês Anterior','newColumn3','Last month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22ff04-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',5,'N',TRUE,'Levantaram no mês','newColumn4','Within this month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a22ff5e-22f1-11ef-9e35-acde48001122','e5bb28c2-708c-11ed-a4c1-acde48001122',6,'PD',TRUE,'Total Pacientes Tratamento','total','Total patients with treatment','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a22ffc2-22f1-11ef-9e35-acde48001122','e5bb267e-708c-11ed-a4c1-acde48001122',0,'PU',TRUE,'Tratamento','treatment','record the quantity of patients for each age group in treatment','USER_INPUT',NULL,'USER_INPUT','9a384fee-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a23001c-22f1-11ef-9e35-acde48001122','e5bb267e-708c-11ed-a4c1-acde48001122',1,'PU',TRUE,'Profilaxia','prophylaxis','record the quantity of patients for each age group in prophylaxis','USER_INPUT',NULL,'USER_INPUT','a4d2a594-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('2a230076-22f1-11ef-9e35-acde48001122','e5bb2a84-708c-11ed-a4c1-acde48001122',0,'TO',TRUE,'Consumo','consumo','record the consumo quantity for each test project','USER_INPUT',NULL,'USER_INPUT','743a0575-6d00-4ff0-89a6-1a76de1c1714',current_setting('vars.templateid')::uuid),
('2a2300d0-22f1-11ef-9e35-acde48001122','e5bb2a84-708c-11ed-a4c1-acde48001122',1,'TO',TRUE,'Positive','positive','record the positive test outcome quantity for each test project','USER_INPUT',NULL,'USER_INPUT','becae41f-1436-4f67-87ac-dece0b97d417',current_setting('vars.templateid')::uuid),
('2a23012a-22f1-11ef-9e35-acde48001122','e5bb2a84-708c-11ed-a4c1-acde48001122',2,'TO',TRUE,'Unjustified','unjustified','record the unjustified test outcome quantity for each test project','USER_INPUT',NULL,'USER_INPUT','fe6e0f40-f47b-41e2-be57-8064876d75f6',current_setting('vars.templateid')::uuid),
('2a23018e-22f1-11ef-9e35-acde48001122','e5bb273c-708c-11ed-a4c1-acde48001122',0,'KD',TRUE,'No. of Kit Received','kitReceived','record the quantity of how many KIT received','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','23c0ecc1-f58e-41e4-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('2a2301e8-22f1-11ef-9e35-acde48001122','e5bb273c-708c-11ed-a4c1-acde48001122',1,'KD',TRUE,'No. of Kit Opened','kitOpened','record the quantity of how many KIT opened','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','86ca8cea-94c2-4d50-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid),
('2a230242-22f1-11ef-9e35-acde48001122','e5bb30b0-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'HF','HF','record the product usage information for my facility','USER_INPUT',NULL,'USER_INPUT','cbee99e4-1827-0291-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('2a23029c-22f1-11ef-9e35-acde48001122','e5bb30b0-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Total','total','record the total number of each column','USER_INPUT',NULL,'USER_INPUT|CALCULATED','95227492-2874-2836-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
('2a230300-22f1-11ef-9e35-acde48001122','e5bb2a3e-708c-11ed-a4c1-acde48001122',0,'TP',TRUE,'HIV Determine','hivDetermine','record the test data for HIV Determine',NULL,NULL,E'','28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',current_setting('vars.templateid')::uuid),
('2a23035a-22f1-11ef-9e35-acde48001122','e5bb2ac0-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'HF','HF','record the test outcome for my facility','USER_INPUT',NULL,'USER_INPUT','c280a232-a39e-4ea9-850b-7bb9fcc2d848',current_setting('vars.templateid')::uuid),
('2a2303b4-22f1-11ef-9e35-acde48001122','e5bb2ac0-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'Total','total','record the total number of each column','USER_INPUT',NULL,'USER_INPUT|CALCULATED','09e5d451-0ffe-43df-ae00-2f15f2a3681b',current_setting('vars.templateid')::uuid),
('2a23040e-22f1-11ef-9e35-acde48001122','e5bb2ac0-708c-11ed-a4c1-acde48001122',2,'SV',TRUE,'APES','APES','record the related test outcomes for APES','USER_INPUT',NULL,'USER_INPUT','379692a8-12f4-4c35-868a-9b6055c8fa8e',current_setting('vars.templateid')::uuid),
('2a230468-22f1-11ef-9e35-acde48001122','e5bb2a02-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Ajuste','new','Ajustment','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a2304cc-22f1-11ef-9e35-acde48001122','e5bb2a02-708c-11ed-a4c1-acde48001122',1,'PD',FALSE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230526-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Novos','new','New','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a230580-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Manutenção','newColumn0','Maintenance','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2305da-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Alteração','newColumn3','Alteration','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230634-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Trânsito','newColumn1','Transit','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2306a2-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',4,'N',TRUE,'Transferências','newColumn2','Transfers','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2306fc-22f1-11ef-9e35-acde48001122','e5bb27be-708c-11ed-a4c1-acde48001122',5,'PD',FALSE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230756-22f1-11ef-9e35-acde48001122','e5bb2944-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Levantaram no mês','new','Within this month','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a2307ba-22f1-11ef-9e35-acde48001122','e5bb2944-708c-11ed-a4c1-acde48001122',1,'PD',TRUE,'Total Pacientes Tratamento','total','Total patients with treatment','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230814-22f1-11ef-9e35-acde48001122','e5bb2840-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'PPE','new','PPE','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a23086e-22f1-11ef-9e35-acde48001122','e5bb2840-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Criança Exposta','newColumn1','Exposed Child','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2308d2-22f1-11ef-9e35-acde48001122','e5bb2840-708c-11ed-a4c1-acde48001122',2,'PD',FALSE,'Total de Pacientes em TARV na US','total','Total patients in ARVT on the HF','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a23092c-22f1-11ef-9e35-acde48001122','e5bb2782-708c-11ed-a4c1-acde48001122',0,'SV',TRUE,'HF','HF','record the quantity of KIT data in my facility',NULL,NULL,E'','cbee99e4-f100-4f9e-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('2a230986-22f1-11ef-9e35-acde48001122','e5bb2782-708c-11ed-a4c1-acde48001122',1,'SV',TRUE,'CHW','CHW','record the quantity of KIT data in CHW','USER_INPUT',NULL,'USER_INPUT','95227492-c394-4f7e-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
('2a2309e0-22f1-11ef-9e35-acde48001122','e5bb29bc-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Total de pacientes com tratamento-DS','new','Total patients with treatment-DS','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a230a44-22f1-11ef-9e35-acde48001122','e5bb29bc-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Total de pacientes com tratamento-DT','newColumn0','Total de pacientes com tratamento-DT','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230a9e-22f1-11ef-9e35-acde48001122','e5bb29bc-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Total de pacientes com tratamento-DB','newColumn2','Total patients with treatment-DB','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230b02-22f1-11ef-9e35-acde48001122','e5bb29bc-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Total de pacientes com tratamento-DM','newColumn1','Total patients with treatment-DM','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230b66-22f1-11ef-9e35-acde48001122','e5bb29bc-708c-11ed-a4c1-acde48001122',4,'PD',TRUE,'Total','total','Total','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230bc0-22f1-11ef-9e35-acde48001122','e5bb28fe-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'2 meses atrás','new','2 months ago','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a230c1a-22f1-11ef-9e35-acde48001122','e5bb28fe-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Mês Anterior','newColumn0','Last month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230c74-22f1-11ef-9e35-acde48001122','e5bb28fe-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Levantaram no mês','newColumn1','Within this month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230cd8-22f1-11ef-9e35-acde48001122','e5bb28fe-708c-11ed-a4c1-acde48001122',3,'PD',TRUE,'Total Pacientes Tratamento','total','Total patients with treatment','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230d32-22f1-11ef-9e35-acde48001122','e5bb2900-708c-11ed-a4c1-acde48001122',0,'N',TRUE,'Mês Anterior','newColumn0','Last month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230d8c-22f1-11ef-9e35-acde48001122','e5bb2900-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Levantaram no mês','newColumn1','Within this month','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a230de6-22f1-11ef-9e35-acde48001122','e5bb2900-708c-11ed-a4c1-acde48001122',2,'PD',TRUE,'Total Pacientes Tratamento','total','Total patients with treatment','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a230e40-22f1-11ef-9e35-acde48001122','e5bb2b06-708c-11ed-a4c1-acde48001122',0,'RE',TRUE,'FNM','code','display the code of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','c47396fe-381a-49a2-887e-ce25c80b0875',current_setting('vars.templateid')::uuid),
('2a230ea4-22f1-11ef-9e35-acde48001122','e5bb2b06-708c-11ed-a4c1-acde48001122',1,'RE',TRUE,'REGIME TERAPÊUTICO','regiment','display the name of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','55353f84-d5a1-4a60-985e-ec3c04212575',current_setting('vars.templateid')::uuid),
('2a230efe-22f1-11ef-9e35-acde48001122','e5bb2b06-708c-11ed-a4c1-acde48001122',2,'RE',TRUE,'Total doentes','patients','record the number of patients','USER_INPUT',NULL,'USER_INPUT','1a44bc23-b652-4a72-b74b-98210d44101c',current_setting('vars.templateid')::uuid),
('2a230f58-22f1-11ef-9e35-acde48001122','e5bb2b06-708c-11ed-a4c1-acde48001122',3,'RE',TRUE,'Farmácia Comunitária','community','record the number of patients in community pharmacy','USER_INPUT',NULL,'USER_INPUT','04065445-3aaf-4928-b329-8454964b62f8',current_setting('vars.templateid')::uuid),
('2a230fbc-22f1-11ef-9e35-acde48001122','e5bb2980-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Mês Corrente-DS','new','Within this month-DS','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a231048-22f1-11ef-9e35-acde48001122','e5bb2980-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Mês Corrente-DT','newColumn0','Within this month-DT','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2310de-22f1-11ef-9e35-acde48001122','e5bb2980-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'Mês Corrente-DB','newColumn2','Within this month-DB','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a23117e-22f1-11ef-9e35-acde48001122','e5bb2980-708c-11ed-a4c1-acde48001122',3,'N',TRUE,'Mês Corrente-DM','newColumn1','Within this month-DM','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a231228-22f1-11ef-9e35-acde48001122','e5bb2980-708c-11ed-a4c1-acde48001122',4,'PD',TRUE,'Total','total','record the total number of this group','CALCULATED',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a2312c8-22f1-11ef-9e35-acde48001122','e5bb2700-708c-11ed-a4c1-acde48001122',0,'CN',TRUE,'No.of External Consultations Performed','consultationNumber','record the number of consultations performed in this period','USER_INPUT',NULL,'USER_INPUT','23c0edc1-9382-7161-99f2-241a3e8360c6',current_setting('vars.templateid')::uuid),
('2a231336-22f1-11ef-9e35-acde48001122','e5bb2700-708c-11ed-a4c1-acde48001122',1,'CN',FALSE,'Total','total','record the total number','USER_INPUT',NULL,'USER_INPUT|CALCULATED','95327492-2874-3836-8fa0-dd2b5ced3e8c',current_setting('vars.templateid')::uuid),
('2a23139a-22f1-11ef-9e35-acde48001122','e5bb287c-708c-11ed-a4c1-acde48001122',0,'PD',TRUE,'Total de pacientes em TARV na US','new','record the number of total de pacientes em TARV na US','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('2a2313f4-22f1-11ef-9e35-acde48001122','e5bb287c-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'Total de Meses de Terapêutica','newColumn0','record the number of total de Meses de Terapêutica','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a231458-22f1-11ef-9e35-acde48001122','e5bb287c-708c-11ed-a4c1-acde48001122',2,'PD',FALSE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('2a2314b2-22f1-11ef-9e35-acde48001122','e5bb2b56-708c-11ed-a4c1-acde48001122',0,'SU',TRUE,'1ª Linha','1stLinhas','display on the second part of the regimen section as the first lines','USER_INPUT',NULL,'USER_INPUT','73a20c66-c0f5-45d3-8268-336198296e33',current_setting('vars.templateid')::uuid),
('2a23150c-22f1-11ef-9e35-acde48001122','e5bb2b56-708c-11ed-a4c1-acde48001122',1,'N',TRUE,'2ª Linha','newColumn0','display on the second part of the regimen section as the second line','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a231570-22f1-11ef-9e35-acde48001122','e5bb2b56-708c-11ed-a4c1-acde48001122',2,'N',TRUE,'3ª Linha','newColumn1','display on the second part of the regimen section as the 3rd line','USER_INPUT',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('2a2315ca-22f1-11ef-9e35-acde48001122','e5bb2b56-708c-11ed-a4c1-acde48001122',3,'SU',TRUE,'Total','total','Count the total number in the second part of the regimen section','CALCULATED',NULL,'CALCULATED|USER_INPUT','676665ea-ba70-4742-b4d3-c512e7a9f389',current_setting('vars.templateid')::uuid),
('2a231624-22f1-11ef-9e35-acde48001122','e5bb2b9c-708c-11ed-a4c1-acde48001122',0,'PU',TRUE,'N Treatments Attended in this Month','treatmentsAttended','record the quantity of patients for each treatment by products','USER_INPUT',NULL,'USER_INPUT','23c0ecc1-9182-7161-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('2a23167e-22f1-11ef-9e35-acde48001122','e5bb2b9c-708c-11ed-a4c1-acde48001122',1,'PU',TRUE,'Existent Stock at the End of the Period','existentStock','record the SOH of the product','USER_INPUT',NULL,'USER_INPUT','86ca8cea-9281-9281-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid);
