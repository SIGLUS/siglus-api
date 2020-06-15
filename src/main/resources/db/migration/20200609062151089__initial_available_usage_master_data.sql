-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO siglusintegration.available_usage_column_sections(id, name, label, displayorder, category) VALUES
('bce979a4-1957-4259-a5bb-b04f86c4c72c','KIT data collection', 'collection', 0, 'KITUSAGE'),
('bce979a4-1957-4259-a5bb-b04f86c4c72a','Services', 'service', 1, 'KITUSAGE');

INSERT INTO siglusintegration.available_usage_columns(id, canbechangedbyuser, canchangeorder, columntype, definition, indicator, isdisplayrequired, label, mandatory, name, supportstag, sectionid, sources, displayorder) VALUES
('23c0ecc1-f58e-41e4-99f2-241a3f8360d6',FALSE,FALSE,'NUMERIC','record the quantity of how many KIT received','KD',TRUE,'No. of Kit Received',FALSE,'kitReceived',true,'bce979a4-1957-4259-a5bb-b04f86c4c72c','USER_INPUT|STOCK_CARDS', 0),
('86ca8cea-94c2-4d50-8dc8-ec5f6ff60ec4',FALSE,TRUE,'NUMERIC','record the quantity of how many KIT opened','KD',FALSE,'No. of Kit Opened',FALSE,'kitOpened',true,'bce979a4-1957-4259-a5bb-b04f86c4c72c','USER_INPUT|STOCK_CARDS', 1),
('cbee99e4-f100-4f9e-ab4f-783d61ac80a6',FALSE,FALSE,'NUMERIC','record the quantity of KIT data in my facility','SV',TRUE,'HF',FALSE,'HF',false,'bce979a4-1957-4259-a5bb-b04f86c4c72a','',0),
('95227492-c394-4f7e-8fa0-dd5b5cef3e8e',FALSE,TRUE,'NUMERIC','record the quantity of KIT data in CHW','SV',FALSE,'CHW',FALSE,'CHW',false,'bce979a4-1957-4259-a5bb-b04f86c4c72a','USER_INPUT',1);


