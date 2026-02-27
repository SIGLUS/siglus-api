-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- for CS
INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '858628a4-8d2b-43ed-a4bd-65ab01f1a5fb', '51ceba02-2a39-457e-a7bb-992c1372eb90', null, 'Consultas Externas/Triagem', 11, 'N', true, 'Consultas Externas/Triagem', 'newColumn8', 'USER_INPUT', null, 'USER_INPUT', 'f0c2d548-648f-11ed-9fc3-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '858628a4-8d2b-43ed-a4bd-65ab01f1a5fb'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'a5a8099c-cf68-40f5-8706-6a10c2b34bef', '51ceba02-2a39-457e-a7bb-992c1372eb90', null, 'SAAJ', 12, 'N', true, 'SAAJ', 'newColumn9', 'USER_INPUT', null, 'USER_INPUT', 'f0c2d548-648f-11ed-9fc3-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'a5a8099c-cf68-40f5-8706-6a10c2b34bef'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '22cd0d94-943e-47a1-8a98-d693adfe0dd7', '51ceba02-2a39-457e-a7bb-992c1372eb90', null, 'CPN/CPP/CCR/CCS', 13, 'N', true, 'CPN/CPP/CCR/CCS', 'newColumn10', 'USER_INPUT', null, 'USER_INPUT', 'f0c2d548-648f-11ed-9fc3-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '22cd0d94-943e-47a1-8a98-d693adfe0dd7'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '8df3e247-9425-4641-8ee9-7657e0637309', '51ceba02-2a39-457e-a7bb-992c1372eb90', null, 'Outros', 14, 'N', true, 'Outros', 'newColumn11', 'USER_INPUT', null, 'USER_INPUT', 'f0c2d548-648f-11ed-9fc3-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '8df3e247-9425-4641-8ee9-7657e0637309'
);

update siglusintegration.usage_columns_maps set displayorder = 15 where id = '2a231d18-22f1-11ef-9e35-acde48001122';
update siglusintegration.usage_columns_maps set displayorder = 16 where id = '2a231d7c-22f1-11ef-9e35-acde48001122';


-- for DPM/AI
INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'ee650d37-dd62-41d2-aa5f-8b3d9ca1d74d', '79b68ab1-648e-11ed-a392-acde48001122', null, 'Consultas Externas/Triagem', 11, 'N', true, 'Consultas Externas/Triagem', 'newColumn8', 'USER_INPUT', null, 'USER_INPUT', '79b68c3e-648e-11ed-a392-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'ee650d37-dd62-41d2-aa5f-8b3d9ca1d74d'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'e910a6cf-d550-401a-aa2b-ef3e232a19d1', '79b68ab1-648e-11ed-a392-acde48001122', null, 'SAAJ', 12, 'N', true, 'SAAJ', 'newColumn9', 'USER_INPUT', null, 'USER_INPUT', '79b68c3e-648e-11ed-a392-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'e910a6cf-d550-401a-aa2b-ef3e232a19d1'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '13160561-799a-4ee0-8bf8-3c0432840d3f', '79b68ab1-648e-11ed-a392-acde48001122', null, 'CPN/CPP/CCR/CCS', 13, 'N', true, 'CPN/CPP/CCR/CCS', 'newColumn10', 'USER_INPUT', null, 'USER_INPUT', '79b68c3e-648e-11ed-a392-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '13160561-799a-4ee0-8bf8-3c0432840d3f'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'e02a4e0a-4ef8-4850-81bd-4397e8103bdf', '79b68ab1-648e-11ed-a392-acde48001122', null, 'Outros', 14, 'N', true, 'Outros', 'newColumn11', 'USER_INPUT', null, 'USER_INPUT', '79b68c3e-648e-11ed-a392-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'e02a4e0a-4ef8-4850-81bd-4397e8103bdf'
);

update siglusintegration.usage_columns_maps set displayorder = 15 where id = '2a232ee8-22f1-11ef-9e35-acde48001122';
update siglusintegration.usage_columns_maps set displayorder = 16 where id = '2a232f7e-22f1-11ef-9e35-acde48001122';


-- for HC
INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '63c64a83-f6c3-45e4-a28b-1305107e0e10', '6ae1309e-92ee-445e-833a-6b696982d4c5', null, 'Consultas Externas/Triagem', 11, 'N', true, 'Consultas Externas/Triagem', 'newColumn8', 'USER_INPUT', null, 'USER_INPUT', 'e5bb32ae-708c-11ed-a4c1-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '63c64a83-f6c3-45e4-a28b-1305107e0e10'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT '0e35cce5-0926-4347-8bd4-a758870ff167', '6ae1309e-92ee-445e-833a-6b696982d4c5', null, 'SAAJ', 12, 'N', true, 'SAAJ', 'newColumn9', 'USER_INPUT', null, 'USER_INPUT', 'e5bb32ae-708c-11ed-a4c1-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = '0e35cce5-0926-4347-8bd4-a758870ff167'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'e271b97e-6c42-48cd-9a22-2607a8d6d2e0', '6ae1309e-92ee-445e-833a-6b696982d4c5', null, 'CPN/CPP/CCR/CCS', 13, 'N', true, 'CPN/CPP/CCR/CCS', 'newColumn10', 'USER_INPUT', null, 'USER_INPUT', 'e5bb32ae-708c-11ed-a4c1-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'e271b97e-6c42-48cd-9a22-2607a8d6d2e0'
);

INSERT INTO siglusintegration.usage_columns_maps (id, requisitiontemplateid, usagecolumnid, definition, displayorder, indicator, isdisplayed, label, name, source, tag, availablesources, usagesectionid)
SELECT 'c89f8bae-0b58-45ea-8c2c-15ed53d70da0', '6ae1309e-92ee-445e-833a-6b696982d4c5', null, 'Outros', 14, 'N', true, 'Outros', 'newColumn11', 'USER_INPUT', null, 'USER_INPUT', 'e5bb32ae-708c-11ed-a4c1-acde48001122'
WHERE NOT EXISTS (
    SELECT 1 FROM siglusintegration.usage_columns_maps WHERE id = 'c89f8bae-0b58-45ea-8c2c-15ed53d70da0'
);

update siglusintegration.usage_columns_maps set displayorder = 15 where id = '2a2340ea-22f1-11ef-9e35-acde48001122';
update siglusintegration.usage_columns_maps set displayorder = 16 where id = '2a234144-22f1-11ef-9e35-acde48001122';
