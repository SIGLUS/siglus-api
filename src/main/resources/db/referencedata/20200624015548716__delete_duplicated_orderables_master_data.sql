-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- a79879b2-d956-11e9-944f-0242ac130005, 04K02, Bicalutamida; 50mg; Comp, Bicalutamida
-- a79eb778-d956-11e9-944f-0242ac130005, 08D0X, Kanamicina; 2 g/8mL-ampola; In, Bicalutamida
DELETE FROM referencedata.trade_items WHERE id IN ('92b75fb4-883c-11ea-a570-4c32759554d9', '92b84370-883c-11ea-a570-4c32759554d9');
DELETE FROM referencedata.orderable_identifiers WHERE orderableid IN ('a79879b2-d956-11e9-944f-0242ac130005', 'a79eb778-d956-11e9-944f-0242ac130005');
DELETE FROM referencedata.program_orderables WHERE orderableid IN ('a79879b2-d956-11e9-944f-0242ac130005', 'a79eb778-d956-11e9-944f-0242ac130005');
DELETE FROM referencedata.orderables WHERE id IN ('a79879b2-d956-11e9-944f-0242ac130005', 'a79eb778-d956-11e9-944f-0242ac130005');
