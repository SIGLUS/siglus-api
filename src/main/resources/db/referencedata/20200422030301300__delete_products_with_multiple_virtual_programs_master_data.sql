-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DELETE FROM referencedata.program_orderables
WHERE id in ('f43637ba-de23-11e9-8785-0242ac130007', '80c9fe46-de24-11e9-8785-0242ac130007', '80c9e8e8-de24-11e9-8785-0242ac130007', '89b31c54-de24-11e9-8785-0242ac130007', '69228baa-de24-11e9-8785-0242ac130007', 'f43665aa-de23-11e9-8785-0242ac130007', 'f439e6a8-de23-11e9-8785-0242ac130007', 'f4369200-de23-11e9-8785-0242ac130007', '211e4934-de24-11e9-8785-0242ac130007');
