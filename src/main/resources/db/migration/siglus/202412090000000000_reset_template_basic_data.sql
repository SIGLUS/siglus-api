DELETE FROM requisition.available_requisition_column_sources;
DELETE FROM requisition.available_requisition_column_options;
DELETE FROM requisition.available_requisition_columns;
DELETE FROM siglusintegration.available_usage_columns;
DELETE FROM siglusintegration.available_usage_column_sections;

-- requisition.available_requisition_columns
INSERT INTO "requisition"."available_requisition_columns"("id","canbechangedbyuser","canchangeorder","columntype","definition","indicator","isdisplayrequired","label","mandatory","name","supportstag")
VALUES
    (E'03d4fe58-12a1-41ce-80b8-d6374342d7ae',FALSE,TRUE,E'NUMERIC',E'Final authorized quantity. This is quantified in dispensing units',E'QA',FALSE,E'Quantidade Autorizada',FALSE,E'authorizedQuantity',FALSE),
    (E'2ed8c74a-f424-4742-bd14-cfbe67b6e7be',FALSE,TRUE,E'TEXT',E'Any additional remarks.',E'L',FALSE,E'Remarks',FALSE,E'remarks',FALSE),
    (E'33b2d2e9-3167-46b0-95d4-1295be9afc22',FALSE,TRUE,E'NUMERIC',E'Based on the Stock On Hand from the previous period. This is quantified in dispensing units.',E'A',FALSE,E'Stock Inicial',FALSE,E'beginningBalance',FALSE),
    (E'4a2e9fd3-1127-4b68-9912-84a5c00f6999',FALSE,TRUE,E'NUMERIC',E'Requested override of calculated quantity. This is quantified in dispensing units.',E'J',FALSE,E'Quantidade Requisitada',FALSE,E'requestedQuantity',FALSE),
    (E'5528576b-b1e7-48d9-bf32-fd0eefefaa9a',FALSE,TRUE,E'NUMERIC',E'Actual quantity needed after deducting stock in hand. This is quantified in dispensing units.',E'I',FALSE,E'Calculated order quantity',FALSE,E'calculatedOrderQuantity',FALSE),
    (E'5708ebf9-9317-4420-85aa-71b2ae92643d',FALSE,TRUE,E'NUMERIC',E'New patients data.',E'F',FALSE,E'Number of new patients added',FALSE,E'numberOfNewPatientsAdded',FALSE),
    (E'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',FALSE,TRUE,E'NUMERIC',E'Total quantity received in the reporting period. This is quantified in dispensing units.',E'B',FALSE,E'Entradas',FALSE,E'totalReceivedQuantity',TRUE),
    (E'61e6d059-10ef-40c4-a6e3-fa7b9ad741ec',FALSE,TRUE,E'TEXT',E'Dispensing unit for this product.',E'U',FALSE,E'Unit/unit of issue',FALSE,E'orderable.dispensable.displayUnit',FALSE),
    (E'6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f5',FALSE,TRUE,E'TEXT',E'Explanation of request for a quantity other than calculated order quantity.',E'W',FALSE,E'Requested quantity explanation',FALSE,E'requestedQuantityExplanation',FALSE),
    (E'720dd95b-b765-4afb-b7f2-7b22261c32f3',FALSE,TRUE,E'NUMERIC',E'Total consumed quantity after adjusting for stockout days. Quantified in dispensing units.',E'N',FALSE,E'Adjusted consumption',FALSE,E'adjustedConsumption',FALSE),
    (E'750b9359-c097-4612-8328-d21671f88920',FALSE,TRUE,E'NUMERIC',E'Total number of days facility was out of stock.',E'X',FALSE,E'Total dias em rutura',FALSE,E'totalStockoutDays',FALSE),
    (E'752cda76-0db5-4b6e-bb79-0f531ab78e2c',FALSE,TRUE,E'NUMERIC',E'Current physical count of stock on hand. This is quantified in dispensing units.',E'E',FALSE,E'Inventário',FALSE,E'stockOnHand',FALSE),
    (E'7e022648-52da-46ab-8001-a0e6388a0964',FALSE,TRUE,E'NUMERIC',E'Theoretical Stock at End of Period = stock at beginning of period + sum of entries -issues',E'TS',FALSE,E'Stock Teorico no Final do Periodo',FALSE,E'theoreticalStockAtEndofPeriod',FALSE),
    (E'878d9a4a-5be8-11ed-acc8-acde48001122',FALSE,TRUE,E'NUMERIC',E'Estimated Quantity',E'EQ',FALSE,E'Quantidade estimada',FALSE,E'estimatedQuantity',FALSE),
    (E'89113ec3-40e9-4d81-9516-b56adba7f8cd',FALSE,TRUE,E'NUMERIC',E'Average consumption over a specified number of periods/months. Quantified in dispensing units.',E'P',FALSE,E'Consumo Médio',FALSE,E'averageConsumption',FALSE),
    (E'90a84767-1009-4d8e-be13-f006fce2a002',TRUE,TRUE,E'NUMERIC',E'Additional quantity required for new patients',E'Z',FALSE,E'Additional quantity required',FALSE,E'additionalQuantityRequired',FALSE),
    (E'913e1a4f-f3b0-40c6-a422-2f73608c6f3d',FALSE,TRUE,E'NUMERIC',E'Maximum stock calculated based on consumption and max stock amounts. Quantified in dispensing units.',E'H',FALSE,E'Maximum stock quantity',FALSE,E'maximumStockQuantity',FALSE),
    (E'96bd0839-4a83-4588-8d78-5566ef80dc89',FALSE,TRUE,E'NUMERIC',E'Difference = Inventory -(Stock at Beginning of Period + Sum of Entries - Issues).',E'DI',FALSE,E'Diferenças',FALSE,E'difference',FALSE),
    (E'9ac91518-38c7-494c-95d9-c37ef2ffff81',TRUE,TRUE,E'NUMERIC',E'Calculated Order Quantity ISA is based on an ISA configured by commodity type, and several trade items may fill for one commodity type.',E'S',FALSE,E'Calc Order Qty ISA',FALSE,E'calculatedOrderQuantityIsa',FALSE),
    (E'9e825396-269d-4873-baa4-89054e2722f4',FALSE,TRUE,E'NUMERIC',E'Quantity dispensed/consumed in the reporting period. This is quantified in dispensing units.',E'C',FALSE,E'Saidas',FALSE,E'totalConsumedQuantity',TRUE),
    (E'a62a5fed-c0b6-4d49-8a96-c631da0d0113',FALSE,TRUE,E'NUMERIC',E'Final approved quantity. This is quantified in dispensing units.',E'K',FALSE,E'Quantidade Aprovada',FALSE,E'approvedQuantity',FALSE),
    (E'aa0a1a7e-e5cb-4385-b781-943316fa116a',TRUE,TRUE,E'NUMERIC',E'The Ideal Stock Amount is the target quantity for a specific commodity type, facility, and period.',E'G',FALSE,E'Ideal Stock Amount',FALSE,E'idealStockAmount',FALSE),
    (E'bde01507-3837-47b7-ae08-cec92c0c3cd2',FALSE,FALSE,E'TEXT',E'Unique identifier for each commodity/product.',E'O',FALSE,E'Código do Produto',FALSE,E'orderable.productCode',FALSE),
    (E'c6dffdee-3813-40d9-8737-f531d5adf420',FALSE,FALSE,E'BOOLEAN',E'Select the check box below to skip a single product. Remove all data from the row prior to selection.',E'S',FALSE,E'Ignorar',FALSE,E'skipped',FALSE),
    (E'cd57f329-f549-4717-882e-ecbf98122c38',FALSE,TRUE,E'NUMERIC',E'All kind of losses/adjustments made at the facility.',E'D',FALSE,E'Total losses and adjustments',FALSE,E'totalLossesAndAdjustments',TRUE),
    (E'd06a04f9-1a81-45df-9921-60adccd0a31e',FALSE,TRUE,E'NUMERIC',E'Suggested quantity calculated for the requisition of DPM.',E'SQ',FALSE,E'Quantidade Sugerida',FALSE,E'suggestedQuantity',FALSE),
    (E'd36a84f9-1a81-45df-1921-60adccd0a31e',FALSE,TRUE,E'TEXT',E'The expiry date of the lot code which will be expired first since today.',E'EX',FALSE,E'Expiration',FALSE,E'expirationDate',FALSE),
    (E'd546e757-5276-4806-87fe-aa10017467d5',FALSE,TRUE,E'NUMERIC',E'Theoretical Quantity to Request = 2 * issues - inventory',E'TQ',FALSE,E'Quantidade Teorica a Requisitar',FALSE,E'theoreticalQuantityToRequest',FALSE),
    (E'dc9dde56-593d-4929-81be-d1faec7025a8',FALSE,TRUE,E'NUMERIC',E'Total packs to be shipped based on pack size and applying rounding rules.',E'V',FALSE,E'Packs to ship',FALSE,E'packsToShip',FALSE),
    (E'df524868-9d0a-18e6-80f5-76304ded7ab9',FALSE,TRUE,E'CURRENCY',E'Price per pack. Will be blank if price is not defined.',E'T',FALSE,E'Price per pack',FALSE,E'pricePerPack',FALSE),
    (E'e3a0c1fc-c2d5-11e6-af2d-3417eb83144e',FALSE,TRUE,E'CURRENCY',E'Total cost of the product based on quantity requested. Will be blank if price is not defined.',E'Q',FALSE,E'Total cost',FALSE,E'totalCost',FALSE),
    (E'e53e80de-fc63-4ecb-b6b2-ef376b34c926',FALSE,FALSE,E'TEXT',E'Primary name of the product.',E'R',TRUE,E'Nome do Produto',FALSE,E'orderable.fullProductName',FALSE),
    (E'ef524868-9d0a-11e6-80f5-76304dec7eb7',FALSE,TRUE,E'NUMERIC',E'Total of beginning balance and quantity received.',E'Y',FALSE,E'Total',FALSE,E'total',FALSE);

--requisition.available_requisition_column_options
INSERT INTO "requisition"."available_requisition_column_options"("id","optionlabel","optionname","columnid")
VALUES
    (E'17d6e860-a746-4500-a0fa-afc84d799dca',E'requisitionConstants.disableSkippedLineItems',E'disableSkippedLineItems',E'c6dffdee-3813-40d9-8737-f531d5adf420'),
    (E'34b8e763-71a0-41f1-86b4-1829963f0704',E'requisitionConstants.newPatientCount',E'newPatientCount',E'5708ebf9-9317-4420-85aa-71b2ae92643d'),
    (E'488e6882-563d-4b69-b7eb-fd59e7772a41',E'requisitionConstants.hideSkippedLineItems',E'hideSkippedLineItems',E'c6dffdee-3813-40d9-8737-f531d5adf420'),
    (E'4957ebb4-297c-459e-a291-812e72286eff',E'requisitionConstants.dispensingUnitsForNewPatients',E'dispensingUnitsForNewPatients',E'5708ebf9-9317-4420-85aa-71b2ae92643d'),
    (E'4b83501e-18cf-44c4-80d4-2dea7f24c1b7',E'requisitionConstants.cp',E'cp',E'd06a04f9-1a81-45df-9921-60adccd0a31e'),
    (E'822fc359-6d78-4ba0-99fd-d7c776041c5e',E'requisitionConstants.cmm',E'cmm',E'd06a04f9-1a81-45df-9921-60adccd0a31e'),
    (E'd1ff8f6f-5bbb-4b0e-8dd2-3835bfc03629',E'requisitionConstants.showPackToShipInApprovalPage',E'showPackToShipInApprovalPage',E'dc9dde56-593d-4929-81be-d1faec7025a8'),
    (E'dcf41f06-3000-4af6-acf5-5de4fffc966f',E'requisitionConstants.showPackToShipInAllPages',E'showPackToShipInAllPages',E'dc9dde56-593d-4929-81be-d1faec7025a8'),
    (E'ff2b350c-37f2-4801-b21e-27ca12c12b3c',E'requisitionConstants.default',E'default',E'913e1a4f-f3b0-40c6-a422-2f73608c6f3d');

--requisition.available_requisition_column_sources
INSERT INTO "requisition"."available_requisition_column_sources"("columnid","value")
VALUES
    (E'4a2e9fd3-1127-4b68-9912-84a5c00f6999',E'USER_INPUT'),
    (E'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',E'USER_INPUT'),
    (E'33b2d2e9-3167-46b0-95d4-1295be9afc22',E'USER_INPUT'),
    (E'752cda76-0db5-4b6e-bb79-0f531ab78e2c',E'USER_INPUT'),
    (E'752cda76-0db5-4b6e-bb79-0f531ab78e2c',E'CALCULATED'),
    (E'9e825396-269d-4873-baa4-89054e2722f4',E'USER_INPUT'),
    (E'9e825396-269d-4873-baa4-89054e2722f4',E'CALCULATED'),
    (E'cd57f329-f549-4717-882e-ecbf98122c38',E'USER_INPUT'),
    (E'6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f5',E'USER_INPUT'),
    (E'2ed8c74a-f424-4742-bd14-cfbe67b6e7be',E'USER_INPUT'),
    (E'bde01507-3837-47b7-ae08-cec92c0c3cd2',E'REFERENCE_DATA'),
    (E'e53e80de-fc63-4ecb-b6b2-ef376b34c926',E'REFERENCE_DATA'),
    (E'a62a5fed-c0b6-4d49-8a96-c631da0d0113',E'USER_INPUT'),
    (E'750b9359-c097-4612-8328-d21671f88920',E'USER_INPUT'),
    (E'ef524868-9d0a-11e6-80f5-76304dec7eb7',E'CALCULATED'),
    (E'61e6d059-10ef-40c4-a6e3-fa7b9ad741ec',E'REFERENCE_DATA'),
    (E'dc9dde56-593d-4929-81be-d1faec7025a8',E'CALCULATED'),
    (E'df524868-9d0a-18e6-80f5-76304ded7ab9',E'REFERENCE_DATA'),
    (E'5708ebf9-9317-4420-85aa-71b2ae92643d',E'USER_INPUT'),
    (E'e3a0c1fc-c2d5-11e6-af2d-3417eb83144e',E'CALCULATED'),
    (E'c6dffdee-3813-40d9-8737-f531d5adf420',E'USER_INPUT'),
    (E'720dd95b-b765-4afb-b7f2-7b22261c32f3',E'CALCULATED'),
    (E'89113ec3-40e9-4d81-9516-b56adba7f8cd',E'CALCULATED'),
    (E'913e1a4f-f3b0-40c6-a422-2f73608c6f3d',E'CALCULATED'),
    (E'5528576b-b1e7-48d9-bf32-fd0eefefaa9a',E'CALCULATED'),
    (E'aa0a1a7e-e5cb-4385-b781-943316fa116a',E'REFERENCE_DATA'),
    (E'752cda76-0db5-4b6e-bb79-0f531ab78e2c',E'STOCK_CARDS'),
    (E'9ac91518-38c7-494c-95d9-c37ef2ffff81',E'CALCULATED'),
    (E'33b2d2e9-3167-46b0-95d4-1295be9afc22',E'STOCK_CARDS'),
    (E'c6dffdee-3813-40d9-8737-f531d5adf420',E'PREVIOUS_REQUISITION'),
    (E'9e825396-269d-4873-baa4-89054e2722f4',E'STOCK_CARDS'),
    (E'5ba8b72d-277a-4da8-b10a-23f0cda23cb4',E'STOCK_CARDS'),
    (E'cd57f329-f549-4717-882e-ecbf98122c38',E'STOCK_CARDS'),
    (E'750b9359-c097-4612-8328-d21671f88920',E'STOCK_CARDS'),
    (E'90a84767-1009-4d8e-be13-f006fce2a002',E'USER_INPUT'),
    (E'89113ec3-40e9-4d81-9516-b56adba7f8cd',E'STOCK_CARDS'),
    (E'7e022648-52da-46ab-8001-a0e6388a0964',E'CALCULATED'),
    (E'd546e757-5276-4806-87fe-aa10017467d5',E'CALCULATED'),
    (E'03d4fe58-12a1-41ce-80b8-d6374342d7ae',E'USER_INPUT'),
    (E'96bd0839-4a83-4588-8d78-5566ef80dc89',E'CALCULATED'),
    (E'd06a04f9-1a81-45df-9921-60adccd0a31e',E'CALCULATED'),
    (E'd36a84f9-1a81-45df-1921-60adccd0a31e',E'CALCULATED'),
    (E'878d9a4a-5be8-11ed-acc8-acde48001122',E'CALCULATED');

-- siglusintegration.available_usage_column_sections
INSERT INTO "siglusintegration"."available_usage_column_sections"("id","name","label","displayorder","category")
VALUES
    (E'0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',E'outcome',E'Test Outcome',1,E'RAPIDTESTCONSUMPTION'),
    (E'28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',E'service',E'Services',2,E'RAPIDTESTCONSUMPTION'),
    (E'43fa95ae-fcfb-11ec-b939-0242ac120002',E'group',E'Faixas Etarias',0,E'AGEGROUP'),
    (E'76a4a700-fcfc-11ec-b939-0242ac120002',E'service',E'Services',1,E'AGEGROUP'),
    (E'91deacfc-1f06-4052-a7d2-0c78a1642043',E'regimen',E'Regimen',0,E'REGIMEN'),
    (E'97230a23-f47b-4115-86fc-f91760b7b439',E'project',E'Test Project',0,E'RAPIDTESTCONSUMPTION'),
    (E'bbf232b4-4b07-4a8e-a22c-1607995b01cd',E'patientType',E'Type of Patient',0,E'PATIENT'),
    (E'bcd979a4-1967-4258-a5be-b04f82c4c62c',E'number',E'Consultation Number',0,E'CONSULTATIONNUMBER'),
    (E'bce979a4-1829-9287-1827-b04f86c4c72a',E'service',E'Services',1,E'USAGEINFORMATION'),
    (E'bce979a4-1876-4259-9102-b04f86c4c72c',E'information',E'Product Usage Information',0,E'USAGEINFORMATION'),
    (E'bce979a4-1957-4259-a5bb-b04f86c4c72a',E'service',E'Services',1,E'KITUSAGE'),
    (E'bce979a4-1957-4259-a5bb-b04f86c4c72c',E'collection',E'KIT data collection',0,E'KITUSAGE'),
    (E'e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',E'summary',E'Summary',1,E'REGIMEN');

-- siglusintegration.available_usage_columns
INSERT INTO "siglusintegration"."available_usage_columns"("id","canbechangedbyuser","canchangeorder","columntype","definition","indicator","isdisplayrequired","label","mandatory","name","supportstag","sectionid","sources","displayorder")
VALUES
    (E'04065445-3aaf-4928-b329-8454964b62f8',FALSE,TRUE,E'NUMERIC',E'record the number of patients in community pharmacy',E'RE',FALSE,E'Farmácia Comunitária',FALSE,E'community',FALSE,E'91deacfc-1f06-4052-a7d2-0c78a1642043',E'USER_INPUT',3),
    (E'07a70f2b-451c-401a-b8ca-75e56baeb91c',FALSE,FALSE,E'NUMERIC',E'record the number of new patients',E'PD',TRUE,E'Novos',FALSE,E'new',FALSE,E'bbf232b4-4b07-4a8e-a22c-1607995b01cd',E'USER_INPUT',0),
    (E'09e5d451-0ffe-43df-ae00-2f15f2a3681b',FALSE,TRUE,E'NUMERIC',E'record the total number of each column',E'SV',TRUE,E'Total',FALSE,E'total',FALSE,E'28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',E'USER_INPUT|CALCULATED',1),
    (E'13263196-fcff-11ec-b939-0242ac120002',FALSE,FALSE,E'NUMERIC',E'Adults',E'SV',TRUE,E'Adultos',FALSE,E'adultos',FALSE,E'76a4a700-fcfc-11ec-b939-0242ac120002',E'USER_INPUT',0),
    (E'1326342a-fcff-11ec-b939-0242ac120002',FALSE,FALSE,E'NUMERIC',E'Child < 25 kg',E'SV',TRUE,E'Criança < 25Kg',FALSE,E'criança < 25Kg',FALSE,E'76a4a700-fcfc-11ec-b939-0242ac120002',E'USER_INPUT',1),
    (E'1a44bc23-b652-4a72-b74b-98210d44101c',FALSE,FALSE,E'NUMERIC',E'record the number of patients',E'RE',TRUE,E'Total doentes',FALSE,E'patients',FALSE,E'91deacfc-1f06-4052-a7d2-0c78a1642043',E'USER_INPUT',2),
    (E'23c0ecc1-9182-7161-99f2-241a3f8360d6',FALSE,FALSE,E'NUMERIC',E'record the quantity of patients for each treatment by products',E'PU',TRUE,E'N Tratamentos atendidos neste mês',FALSE,E'treatmentsAttended',FALSE,E'bce979a4-1876-4259-9102-b04f86c4c72c',E'USER_INPUT',0),
    (E'23c0ecc1-f58e-41e4-99f2-241a3f8360d6',FALSE,FALSE,E'NUMERIC',E'record the quantity of how many KIT received',E'KD',TRUE,E'No. de Kits Recebidos',FALSE,E'kitReceived',TRUE,E'bce979a4-1957-4259-a5bb-b04f86c4c72c',E'USER_INPUT|STOCK_CARDS',0),
    (E'23c0edc1-9382-7161-99f2-241a3e8360c6',FALSE,FALSE,E'NUMERIC',E'record the number of consultations performed in this period',E'CN',TRUE,E'Nº de Consultas Externas Realizadas',FALSE,E'consultationNumber',FALSE,E'bcd979a4-1967-4258-a5be-b04f82c4c62c',E'USER_INPUT',0),
    (E'28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',FALSE,FALSE,E'NUMERIC',E'record the test data for HIV Determine',E'TP',TRUE,E'HIV Determine',FALSE,E'hivDetermine',FALSE,E'97230a23-f47b-4115-86fc-f91760b7b439',E'',0),
    (E'379692a8-12f4-4c35-868a-9b6055c8fa8e',FALSE,TRUE,E'NUMERIC',E'record the related test outcomes for APES',E'SV',FALSE,E'APES',FALSE,E'APES',FALSE,E'28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',E'USER_INPUT',2),
    (E'55353f84-d5a1-4a60-985e-ec3c04212575',FALSE,FALSE,E'TEXT',E'display the name of each regimen',E'RE',TRUE,E'Regimes Terapeuticos',FALSE,E'regiment',FALSE,E'91deacfc-1f06-4052-a7d2-0c78a1642043',E'REFERENCE_DATA',1),
    (E'676665ea-ba70-4742-b4d3-c512e7a9f389',FALSE,TRUE,E'NUMERIC',E'Count the total number in the second part of the regimen section',E'SU',TRUE,E'Total',FALSE,E'total',FALSE,E'e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',E'CALCULATED|USER_INPUT',1),
    (E'73a20c66-c0f5-45d3-8268-336198296e33',FALSE,FALSE,E'NUMERIC',E'display on the second part of the regimen section as the first lines',E'SU',TRUE,E'1st linhas',FALSE,E'1stLinhas',FALSE,E'e2cd8d9e-22e9-4ea4-a61c-e3bd603d02cb',E'USER_INPUT',0),
    (E'743a0575-6d00-4ff0-89a6-1a76de1c1714',FALSE,FALSE,E'NUMERIC',E'record the consumo quantity for each test project',E'TO',TRUE,E'Consumo',FALSE,E'consumo',FALSE,E'0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',E'USER_INPUT',0),
    (E'86ca8cea-9281-9281-8dc8-ec5f6ff60ec4',FALSE,TRUE,E'NUMERIC',E'record the SOH of the product',E'PU',FALSE,E'Stock Existente no Final do Período',FALSE,E'existentStock',FALSE,E'bce979a4-1876-4259-9102-b04f86c4c72c',E'USER_INPUT',1),
    (E'86ca8cea-94c2-4d50-8dc8-ec5f6ff60ec4',FALSE,TRUE,E'NUMERIC',E'record the quantity of how many KIT opened and issued',E'KD',FALSE,E'Nº de Kits Abertos e Enviados',FALSE,E'kitOpened',TRUE,E'bce979a4-1957-4259-a5bb-b04f86c4c72c',E'USER_INPUT|STOCK_CARDS',1),
    (E'95227492-2874-2836-8fa0-dd5b5cef3e8e',FALSE,TRUE,E'NUMERIC',E'record the total number of each column',E'SV',TRUE,E'Total',FALSE,E'total',TRUE,E'bce979a4-1829-9287-1827-b04f86c4c72a',E'USER_INPUT|CALCULATED',1),
    (E'95227492-c394-4f7e-8fa0-dd5b5cef3e8e',FALSE,TRUE,E'NUMERIC',E'record the quantity of KIT data in CHW',E'SV',FALSE,E'CHW',FALSE,E'CHW',FALSE,E'bce979a4-1957-4259-a5bb-b04f86c4c72a',E'USER_INPUT',1),
    (E'95327492-2874-3836-8fa0-dd2b5ced3e8c',FALSE,TRUE,E'NUMERIC',E'record the total number',E'CN',FALSE,E'Total',FALSE,E'total',TRUE,E'bcd979a4-1967-4258-a5be-b04f82c4c62c',E'USER_INPUT|CALCULATED',1),
    (E'9a384fee-fcfe-11ec-b939-0242ac120002',FALSE,FALSE,E'NUMERIC',E'record the quantity of patients for each age group in treatment',E'PU',TRUE,E'Tratamento',FALSE,E'treatment',FALSE,E'43fa95ae-fcfb-11ec-b939-0242ac120002',E'USER_INPUT',0),
    (E'a4d2a594-fcfe-11ec-b939-0242ac120002',FALSE,FALSE,E'NUMERIC',E'record the quantity of patients for each age group in prophylaxis',E'PU',TRUE,E'Profilaxia',FALSE,E'prophylaxis',FALSE,E'43fa95ae-fcfb-11ec-b939-0242ac120002',E'USER_INPUT',1),
    (E'becae41f-1436-4f67-87ac-dece0b97d417',FALSE,FALSE,E'NUMERIC',E'record the positive test outcome quantity for each test project',E'TO',FALSE,E'Positive',FALSE,E'positive',FALSE,E'0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',E'USER_INPUT',1),
    (E'c1092b36-fd01-11ec-b939-0242ac120002',FALSE,FALSE,E'NUMERIC',E'Child > 25 kg',E'SV',TRUE,E'Criança > 25Kg',FALSE,E'criança > 25Kg',FALSE,E'76a4a700-fcfc-11ec-b939-0242ac120002',E'USER_INPUT',2),
    (E'c280a232-a39e-4ea9-850b-7bb9fcc2d848',FALSE,FALSE,E'NUMERIC',E'record the test outcome for my facility',E'SV',TRUE,E'HF',FALSE,E'HF',FALSE,E'28cbdef0-318b-4b34-a4c3-9d1b5bb74d15',E'USER_INPUT',0),
    (E'c47396fe-381a-49a2-887e-ce25c80b0875',FALSE,FALSE,E'TEXT',E'display the code of each regimen',E'RE',FALSE,E'Código',FALSE,E'code',FALSE,E'91deacfc-1f06-4052-a7d2-0c78a1642043',E'REFERENCE_DATA',0),
    (E'cbee99e4-1827-0291-ab4f-783d61ac80a6',FALSE,FALSE,E'NUMERIC',E'record the product usage information for my facility',E'SV',TRUE,E'HF',FALSE,E'HF',FALSE,E'bce979a4-1829-9287-1827-b04f86c4c72a',E'USER_INPUT',0),
    (E'cbee99e4-f100-4f9e-ab4f-783d61ac80a6',FALSE,FALSE,E'NUMERIC',E'record the quantity of KIT data in my facility',E'SV',TRUE,E'HF',FALSE,E'HF',FALSE,E'bce979a4-1957-4259-a5bb-b04f86c4c72a',E'',0),
    (E'f51371a4-ba6f-4119-9a0e-1a588fa5df21',FALSE,TRUE,E'NUMERIC',E'record the total number of this group',E'PD',FALSE,E'Total',FALSE,E'total',FALSE,E'bbf232b4-4b07-4a8e-a22c-1607995b01cd',E'USER_INPUT|CALCULATED',1),
    (E'fe6e0f40-f47b-41e2-be57-8064876d75f6',FALSE,FALSE,E'NUMERIC',E'record the unjustified test outcome quantity for each test project',E'TO',FALSE,E'Unjustified',FALSE,E'unjustified',FALSE,E'0d3b5adc-e767-49a1-8ed7-7b2a92f82d7f',E'USER_INPUT',2);
