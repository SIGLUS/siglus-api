-- Malaria Template - DPM/AI
SET local vars.template = 'Malaria Template - DPM/AI';
SET local vars.templateid = 'fe386d97-941a-43bd-8ce0-f13867c7dd2d';
SET local vars.programid = 'ad8c9a54-cc66-4395-a353-1849caec87da';

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
        WHERE ft.code IN ('DPM','AI')
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
INSERT INTO requisition.requisition_template_assignments(id,programid,templateid,facilitytypeid) VALUES
('2a23d1d6-22f1-11ef-9e35-acde48001122',current_setting('vars.programid')::uuid,current_setting('vars.templateid')::uuid,(SELECT id FROM referencedata.facility_types ft WHERE ft.code = 'DPM')),
('2a23d230-22f1-11ef-9e35-acde48001122',current_setting('vars.programid')::uuid,current_setting('vars.templateid')::uuid,(SELECT id FROM referencedata.facility_types ft WHERE ft.code = 'AI'));

-- requisition_template_extension
INSERT INTO siglusintegration.requisition_template_extension(id,requisitiontemplateid,enableconsultationnumber,enablekitusage,enableproduct,enablepatientlineitem,enableregimen,enablerapidtestconsumption,enableusageinformation,enablequicklyfill,enableagegroup) VALUES
('feacf109-45fa-4ec9-be1c-df339c865341',current_setting('vars.templateid')::uuid,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,TRUE,FALSE);

-- columns_maps
-- SOURCE (0: USER_INPUT, 1: CALCULATED , 2: REFERENCE_DATA, 3: STOCK_CARDS, 4: PREVIOUS_REQUISITION)
INSERT INTO requisition.columns_maps(displayorder,indicator,isdisplayed,key,label,name,source,definition,tag,requisitioncolumnoptionid,requisitioncolumnid,requisitiontemplateid) VALUES
(0,'S',TRUE,'skipped','Skip','skipped',4,'Select the check box below to skip a single product. Remove all data from the row prior to selection.',NULL,'17d6e860-a746-4500-a0fa-afc84d799dca','c6dffdee-3813-40d9-8737-f531d5adf420',current_setting('vars.templateid')::uuid),
(1,'O',TRUE,'orderable.productCode','Product code','orderable.productCode',2,'Product code',NULL,NULL,'bde01507-3837-47b7-ae08-cec92c0c3cd2',current_setting('vars.templateid')::uuid),
(2,'R',TRUE,'orderable.fullProductName','Product Name','orderable.fullProductName',2,'Product name',NULL,NULL,'e53e80de-fc63-4ecb-b6b2-ef376b34c926',current_setting('vars.templateid')::uuid),
(3,'A',TRUE,'beginningBalance','Stock at Beginning of Period','beginningBalance',3,'Initial stock quantity',NULL,NULL,'33b2d2e9-3167-46b0-95d4-1295be9afc22',current_setting('vars.templateid')::uuid),
(4,'B',TRUE,'totalReceivedQuantity','Sum of Entries','totalReceivedQuantity',3,'Received quantity','received',NULL,'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',current_setting('vars.templateid')::uuid),
(5,'C',TRUE,'totalConsumedQuantity','Issues','totalConsumedQuantity',3,'Issued quantity','consumed',NULL,'9e825396-269d-4873-baa4-89054e2722f4',current_setting('vars.templateid')::uuid),
(6,'TS',TRUE,'theoreticalStockAtEndofPeriod','Theoretical Stock at End of Period','theoreticalStockAtEndofPeriod',1,'Theoretical Stock at End of Period = stock at beginning of period + sum of entries -issues',NULL,NULL,'7e022648-52da-46ab-8001-a0e6388a0964',current_setting('vars.templateid')::uuid),
(7,'E',TRUE,'stockOnHand','Inventory','stockOnHand',3,'Current stock on hand',NULL,NULL,'752cda76-0db5-4b6e-bb79-0f531ab78e2c',current_setting('vars.templateid')::uuid),
(8,'DI',TRUE,'difference','Difference','difference',1,'Difference = Inventory -(Stock at Beginning of Period + Sum of Entries - Issues)',NULL,NULL,'96bd0839-4a83-4588-8d78-5566ef80dc89',current_setting('vars.templateid')::uuid),
(9,'X',TRUE,'totalStockoutDays','Total stockout days','totalStockoutDays',3,'Total number of days facility was out of stock.',NULL,NULL,'750b9359-c097-4612-8328-d21671f88920',current_setting('vars.templateid')::uuid),
(10,'P',TRUE,'averageConsumption','Average consumption','averageConsumption',3,'Average consumption over a specified number of periods/months. Quantified in dispensing units.',NULL,NULL,'89113ec3-40e9-4d81-9516-b56adba7f8cd',current_setting('vars.templateid')::uuid),
(11,'TQ',TRUE,'theoreticalQuantityToRequest','Theoretical Quantity to Request','theoreticalQuantityToRequest',1,'Theoretical Quantity to Request = 2 * issues - inventory',NULL,NULL,'d546e757-5276-4806-87fe-aa10017467d5',current_setting('vars.templateid')::uuid),
(12,'J',TRUE,'requestedQuantity','Requested quantity','requestedQuantity',0,'Requested quantity',NULL,NULL,'4a2e9fd3-1127-4b68-9912-84a5c00f6999',current_setting('vars.templateid')::uuid),
(13,'QA',TRUE,'authorizedQuantity','Quantity Authorized','authorizedQuantity',0,'Final authorized quantity',NULL,NULL,'03d4fe58-12a1-41ce-80b8-d6374342d7ae',current_setting('vars.templateid')::uuid),
(100,'Z',FALSE,'additionalQuantityRequired','Additional quantity required','additionalQuantityRequired',0,'Additional quantity required for new patients',NULL,NULL,'90a84767-1009-4d8e-be13-f006fce2a002',current_setting('vars.templateid')::uuid),
(100,'N',FALSE,'adjustedConsumption','Adjusted consumption','adjustedConsumption',1,'Total consumed quantity after adjusting for stockout days. Quantified in dispensing units.',NULL,NULL,'720dd95b-b765-4afb-b7f2-7b22261c32f3',current_setting('vars.templateid')::uuid),
(100,'K',TRUE,'approvedQuantity','Quantity Approved','approvedQuantity',0,'Final approved quantity',NULL,NULL,'a62a5fed-c0b6-4d49-8a96-c631da0d0113',current_setting('vars.templateid')::uuid),
(100,'I',FALSE,'calculatedOrderQuantity','Calculated order quantity','calculatedOrderQuantity',1,'Actual quantity needed after deducting stock in hand. This is quantified in dispensing units.',NULL,NULL,'5528576b-b1e7-48d9-bf32-fd0eefefaa9a',current_setting('vars.templateid')::uuid),
(100,'S',FALSE,'calculatedOrderQuantityIsa','Calc Order Qty ISA','calculatedOrderQuantityIsa',1,'Calculated Order Quantity ISA is based on an ISA configured by commodity type, and several trade items may fill for one commodity type.',NULL,NULL,'9ac91518-38c7-494c-95d9-c37ef2ffff81',current_setting('vars.templateid')::uuid),
(100,'EQ',FALSE,'estimatedQuantity','Quantidade Estimada','estimatedQuantity',1,'Estimated Quantity',NULL,NULL,'878d9a4a-5be8-11ed-acc8-acde48001122',current_setting('vars.templateid')::uuid),
(100,'EX',FALSE,'expirationDate','Expiration','expirationDate',1,'Date of the lot  which will be expired first since today',NULL,NULL,'d36a84f9-1a81-45df-1921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(100,'G',FALSE,'idealStockAmount','Ideal Stock Amount','idealStockAmount',2,'The Ideal Stock Amount is the target quantity for a specific commodity type, facility, and period.',NULL,NULL,'aa0a1a7e-e5cb-4385-b781-943316fa116a',current_setting('vars.templateid')::uuid),
(100,'H',FALSE,'maximumStockQuantity','Maximum stock quantity','maximumStockQuantity',1,'Maximum stock calculated based on consumption and max stock amounts. Quantified in dispensing units.',NULL,'ff2b350c-37f2-4801-b21e-27ca12c12b3c','913e1a4f-f3b0-40c6-a422-2f73608c6f3d',current_setting('vars.templateid')::uuid),
(100,'F',FALSE,'numberOfNewPatientsAdded','Number of new patients added','numberOfNewPatientsAdded',0,'New patients data.',NULL,'4957ebb4-297c-459e-a291-812e72286eff','5708ebf9-9317-4420-85aa-71b2ae92643d',current_setting('vars.templateid')::uuid),
(100,'U',FALSE,'orderable.dispensable.displayUnit','Unit/unit of issue','orderable.dispensable.displayUnit',2,'Product unit',NULL,NULL,'61e6d059-10ef-40c4-a6e3-fa7b9ad741ec',current_setting('vars.templateid')::uuid),
(100,'V',FALSE,'packsToShip','Packs to ship','packsToShip',1,'Total packs to be shipped based on pack size and applying rounding rules.',NULL,'dcf41f06-3000-4af6-acf5-5de4fffc966f','dc9dde56-593d-4929-81be-d1faec7025a8',current_setting('vars.templateid')::uuid),
(100,'T',FALSE,'pricePerPack','Price per pack','pricePerPack',2,'Price per pack. Will be blank if price is not defined.',NULL,NULL,'df524868-9d0a-18e6-80f5-76304ded7ab9',current_setting('vars.templateid')::uuid),
(100,'L',FALSE,'remarks','Remarks','remarks',0,'Any additional remarks.',NULL,NULL,'2ed8c74a-f424-4742-bd14-cfbe67b6e7be',current_setting('vars.templateid')::uuid),
(100,'W',FALSE,'requestedQuantityExplanation','Requested quantity explanation','requestedQuantityExplanation',0,'Explanation of request for a quantity other than calculated order quantity.',NULL,NULL,'6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f5',current_setting('vars.templateid')::uuid),
(100,'SQ',FALSE,'suggestedQuantity','Suggested Quantity','suggestedQuantity',1,'Suggested quantity by FC',NULL,'822fc359-6d78-4ba0-99fd-d7c776041c5e','d06a04f9-1a81-45df-9921-60adccd0a31e',current_setting('vars.templateid')::uuid),
(100,'Y',FALSE,'total','Total','total',1,'Total of beginning balance and quantity received.',NULL,NULL,'ef524868-9d0a-11e6-80f5-76304dec7eb7',current_setting('vars.templateid')::uuid),
(100,'Q',FALSE,'totalCost','Total cost','totalCost',1,'Total cost of the product based on quantity requested. Will be blank if price is not defined.',NULL,NULL,'e3a0c1fc-c2d5-11e6-af2d-3417eb83144e',current_setting('vars.templateid')::uuid),
(100,'D',FALSE,'totalLossesAndAdjustments','Total losses and adjustments','totalLossesAndAdjustments',3,'Adjustment quantity','adjustment',NULL,'cd57f329-f549-4717-882e-ecbf98122c38',current_setting('vars.templateid')::uuid);

-- usage_sections_maps
INSERT INTO siglusintegration.usage_sections_maps(id,category,name,label,displayorder,sectionid,requisitiontemplateid) VALUES
('216557e2-63f1-11ed-bf1b-acde48001122','AGEGROUP','group','Faixas Etarias',0,'43fa95ae-fcfb-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('21655828-63f1-11ed-bf1b-acde48001122','AGEGROUP','service','Services',1,'bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('2165597c-63f1-11ed-bf1b-acde48001122','CONSULTATIONNUMBER','number','Consultation Number',0,'bcd979a4-1967-4258-a5be-b04f82c4c62c',current_setting('vars.templateid')::uuid),
('21655a26-63f1-11ed-bf1b-acde48001122','KITUSAGE','service','Services',1,'bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('21655a80-63f1-11ed-bf1b-acde48001122','KITUSAGE','collection','KIT data collection',0,'bce979a4-1957-4259-a5bb-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('21655ac6-63f1-11ed-bf1b-acde48001122','PATIENT','patientType','Type of Patient',0,'bbf232b4-4b07-4a8e-a22c-1607995b01cd',current_setting('vars.templateid')::uuid),
('21655b0c-63f1-11ed-bf1b-acde48001122','RAPIDTESTCONSUMPTION','outcome','Test Outcome',1,'0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',current_setting('vars.templateid')::uuid),
('21655b52-63f1-11ed-bf1b-acde48001122','RAPIDTESTCONSUMPTION','service','Services',2,'bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid),
('21655b8e-63f1-11ed-bf1b-acde48001122','RAPIDTESTCONSUMPTION','project','Test Project',0,'97230a23-f47b-4115-86fc-f91760b7b439',current_setting('vars.templateid')::uuid),
('21655bca-63f1-11ed-bf1b-acde48001122','REGIMEN','summary','Summary',1,'e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',current_setting('vars.templateid')::uuid),
('21655c10-63f1-11ed-bf1b-acde48001122','REGIMEN','regimen','Regimen',0,'91deacfc-1f06-4052-a7d2-0c78a1642043',current_setting('vars.templateid')::uuid),
('21655760-63f1-11ed-bf1b-acde48001122','USAGEINFORMATION','information','Product Usage Information',0,'bce979a4-1876-4259-9102-b04f86c4c72c',current_setting('vars.templateid')::uuid),
('2165579c-63f1-11ed-bf1b-acde48001122','USAGEINFORMATION','service','Services',1,'bce979a4-1957-4259-a5bb-b04f86c4c72a',current_setting('vars.templateid')::uuid);

-- usage_columns_maps
INSERT INTO siglusintegration.usage_columns_maps(id,usagesectionid,displayorder,indicator,isdisplayed,label,name,definition,source,tag,availablesources,usagecolumnid,requisitiontemplateid) VALUES
('ae67326e-640a-11ed-ae59-acde48001122','21655760-63f1-11ed-bf1b-acde48001122',0,'PU',TRUE,'Nº Tratamentos Aviados no Mês','treatmentsAttended','record the quantity of patients for each treatment by products','STOCK_CARDS',NULL,'USER_INPUT','23c0ecc1-9182-7161-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('ae67348a-640a-11ed-ae59-acde48001122','21655760-63f1-11ed-bf1b-acde48001122',1,'PU',TRUE,'Stock Existente no Fim do período','existentStock','record the SOH of the product','STOCK_CARDS',NULL,'USER_INPUT','86ca8cea-9281-9281-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid),
('ae673520-640a-11ed-ae59-acde48001122','2165579c-63f1-11ed-bf1b-acde48001122',0,'SV',TRUE,'US','HF','record the product usage information for my facility','STOCK_CARDS',NULL,'USER_INPUT','cbee99e4-1827-0291-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('ae67358e-640a-11ed-ae59-acde48001122','2165579c-63f1-11ed-bf1b-acde48001122',1,'N',TRUE,'APE','newColumn0','CHW','STOCK_CARDS',NULL,'USER_INPUT',NULL,current_setting('vars.templateid')::uuid),
('ae6735f2-640a-11ed-ae59-acde48001122','2165579c-63f1-11ed-bf1b-acde48001122',2,'SV',TRUE,'Total','total','record the total number of each column','CALCULATED',NULL,'USER_INPUT|CALCULATED','95227492-2874-2836-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
-- unused
('ae673642-640a-11ed-ae59-acde48001122','21655a26-63f1-11ed-bf1b-acde48001122',0,'SV',TRUE,'HF','HF','record the quantity of KIT data in my facility',NULL,NULL,E'','cbee99e4-f100-4f9e-ab4f-783d61ac80a6',current_setting('vars.templateid')::uuid),
('ae67369c-640a-11ed-ae59-acde48001122','21655a26-63f1-11ed-bf1b-acde48001122',1,'SV',TRUE,'CHW','CHW','record the quantity of KIT data in CHW','USER_INPUT',NULL,'USER_INPUT','95227492-c394-4f7e-8fa0-dd5b5cef3e8e',current_setting('vars.templateid')::uuid),
('ae6736ec-640a-11ed-ae59-acde48001122','21655b0c-63f1-11ed-bf1b-acde48001122',0,'TO',TRUE,'Consumo','consumo','record the consumo quantity for each test project','USER_INPUT',NULL,'USER_INPUT','743a0575-6d00-4ff0-89a6-1a76de1c1714',current_setting('vars.templateid')::uuid),
('ae673746-640a-11ed-ae59-acde48001122','21655b0c-63f1-11ed-bf1b-acde48001122',1,'TO',TRUE,'Positive','positive','record the positive test outcome quantity for each test project','USER_INPUT',NULL,'USER_INPUT','becae41f-1436-4f67-87ac-dece0b97d417',current_setting('vars.templateid')::uuid),
('ae673796-640a-11ed-ae59-acde48001122','21655b0c-63f1-11ed-bf1b-acde48001122',2,'TO',TRUE,'Unjustified','unjustified','record the unjustified test outcome quantity for each test project','USER_INPUT',NULL,'USER_INPUT','fe6e0f40-f47b-41e2-be57-8064876d75f6',current_setting('vars.templateid')::uuid),
('ae6737f0-640a-11ed-ae59-acde48001122','21655a80-63f1-11ed-bf1b-acde48001122',0,'KD',TRUE,'No. of Kit Received','kitReceived','record the quantity of how many KIT received','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','23c0ecc1-f58e-41e4-99f2-241a3f8360d6',current_setting('vars.templateid')::uuid),
('ae673840-640a-11ed-ae59-acde48001122','21655a80-63f1-11ed-bf1b-acde48001122',1,'KD',TRUE,'No. of Kit Opened','kitOpened','record the quantity of how many KIT opened','USER_INPUT',NULL,'USER_INPUT|STOCK_CARDS','86ca8cea-94c2-4d50-8dc8-ec5f6ff60ec4',current_setting('vars.templateid')::uuid),
('ae67389a-640a-11ed-ae59-acde48001122','216557e2-63f1-11ed-bf1b-acde48001122',0,'PU',TRUE,'Tratamento','treatment','record the quantity of patients for each age group in treatment','USER_INPUT',NULL,'USER_INPUT','9a384fee-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('ae6738ea-640a-11ed-ae59-acde48001122','216557e2-63f1-11ed-bf1b-acde48001122',1,'PU',TRUE,'Profilaxia','prophylaxis','record the quantity of patients for each age group in prophylaxis','USER_INPUT',NULL,'USER_INPUT','a4d2a594-fcfe-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('ae67393a-640a-11ed-ae59-acde48001122','2165597c-63f1-11ed-bf1b-acde48001122',0,'CN',TRUE,'No.of External Consultations Performed','consultationNumber','record the number of consultations performed in this period','USER_INPUT',NULL,'USER_INPUT','23c0edc1-9382-7161-99f2-241a3e8360c6',current_setting('vars.templateid')::uuid),
('ae673994-640a-11ed-ae59-acde48001122','2165597c-63f1-11ed-bf1b-acde48001122',1,'CN',FALSE,'Total','total','record the total number','USER_INPUT',NULL,'USER_INPUT|CALCULATED','95327492-2874-3836-8fa0-dd2b5ced3e8c',current_setting('vars.templateid')::uuid),
('ae6739e4-640a-11ed-ae59-acde48001122','21655828-63f1-11ed-bf1b-acde48001122',0,'SV',TRUE,'Adultos','adultos','Adults','USER_INPUT',NULL,'USER_INPUT','13263196-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('ae673a3e-640a-11ed-ae59-acde48001122','21655828-63f1-11ed-bf1b-acde48001122',1,'SV',TRUE,'Criança < 25Kg','criança < 25Kg','Child < 25 kg','USER_INPUT',NULL,'USER_INPUT','1326342a-fcff-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('ae673a8e-640a-11ed-ae59-acde48001122','21655828-63f1-11ed-bf1b-acde48001122',2,'SV',TRUE,'Criança > 25Kg','criança > 25Kg','Child > 25 kg','USER_INPUT',NULL,'USER_INPUT','c1092b36-fd01-11ec-b939-0242ac120002',current_setting('vars.templateid')::uuid),
('ae673ae8-640a-11ed-ae59-acde48001122','21655bca-63f1-11ed-bf1b-acde48001122',0,'SU',TRUE,'1st linhas','1stLinhas','display on the second part of the regimen section as the first lines','USER_INPUT',NULL,'USER_INPUT','73a20c66-c0f5-45d3-8268-336198296e33',current_setting('vars.templateid')::uuid),
('ae673b38-640a-11ed-ae59-acde48001122','21655bca-63f1-11ed-bf1b-acde48001122',1,'SU',TRUE,'Total','total','Count the total number in the second part of the regimen section','CALCULATED',NULL,'CALCULATED|USER_INPUT','676665ea-ba70-4742-b4d3-c512e7a9f389',current_setting('vars.templateid')::uuid),
('ae673b92-640a-11ed-ae59-acde48001122','21655b52-63f1-11ed-bf1b-acde48001122',0,'SV',TRUE,'HF','HF','record the test outcome for my facility','USER_INPUT',NULL,'USER_INPUT','c280a232-a39e-4ea9-850b-7bb9fcc2d848',current_setting('vars.templateid')::uuid),
('ae673be2-640a-11ed-ae59-acde48001122','21655b52-63f1-11ed-bf1b-acde48001122',1,'SV',TRUE,'Total','total','record the total number of each column','USER_INPUT',NULL,'USER_INPUT|CALCULATED','09e5d451-0ffe-43df-ae00-2f15f2a3681b',current_setting('vars.templateid')::uuid),
('ae673c3c-640a-11ed-ae59-acde48001122','21655b52-63f1-11ed-bf1b-acde48001122',2,'SV',TRUE,'APES','APES','record the related test outcomes for APES','USER_INPUT',NULL,'USER_INPUT','379692a8-12f4-4c35-868a-9b6055c8fa8e',current_setting('vars.templateid')::uuid),
('ae673c8c-640a-11ed-ae59-acde48001122','21655b8e-63f1-11ed-bf1b-acde48001122',0,'TP',TRUE,'HIV Determine','hivDetermine','record the test data for HIV Determine',NULL,NULL,E'','28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',current_setting('vars.templateid')::uuid),
('ae673cdc-640a-11ed-ae59-acde48001122','21655ac6-63f1-11ed-bf1b-acde48001122',0,'PD',TRUE,'New','new','record the number of new patients','USER_INPUT',NULL,'USER_INPUT','07a70f2b-451c-401a-b8ca-75e56baeb91c',current_setting('vars.templateid')::uuid),
('ae673d36-640a-11ed-ae59-acde48001122','21655ac6-63f1-11ed-bf1b-acde48001122',1,'PD',TRUE,'Total','total','record the total number of this group','USER_INPUT',NULL,'USER_INPUT|CALCULATED','f51371a4-ba6f-4119-9a0e-1a588fa5df21',current_setting('vars.templateid')::uuid),
('ae673d86-640a-11ed-ae59-acde48001122','21655c10-63f1-11ed-bf1b-acde48001122',0,'RE',TRUE,'Code','code','display the code of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','c47396fe-381a-49a2-887e-ce25c80b0875',current_setting('vars.templateid')::uuid),
('ae673de0-640a-11ed-ae59-acde48001122','21655c10-63f1-11ed-bf1b-acde48001122',1,'RE',TRUE,'Therapeutic regiment','regiment','display the name of each regimen','REFERENCE_DATA',NULL,'REFERENCE_DATA','55353f84-d5a1-4a60-985e-ec3c04212575',current_setting('vars.templateid')::uuid),
('ae673e30-640a-11ed-ae59-acde48001122','21655c10-63f1-11ed-bf1b-acde48001122',2,'RE',TRUE,'Total patients','patients','record the number of patients','USER_INPUT',NULL,'USER_INPUT','1a44bc23-b652-4a72-b74b-98210d44101c',current_setting('vars.templateid')::uuid),
('ae673e8a-640a-11ed-ae59-acde48001122','21655c10-63f1-11ed-bf1b-acde48001122',3,'RE',TRUE,'Community pharmacy','community','record the number of patients in community pharmacy','USER_INPUT',NULL,'USER_INPUT','04065445-3aaf-4928-b329-8454964b62f8',current_setting('vars.templateid')::uuid);
