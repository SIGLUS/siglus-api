-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "stockmanagement"."stock_card_line_item_reasons"("id","description","isfreetextallowed","name","reasoncategory","reasontype")
VALUES
('44814746-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Loans received at the health facility deposit','ADJUSTMENT','CREDIT'),
('448147f0-df64-11e9-9e7e-4c32759554d9',NULL,TRUE,'Positive Correction','ADJUSTMENT','CREDIT'),
('448148b8-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Returns from Customers(HF and dependent wards)','ADJUSTMENT','CREDIT'),
('44814a0c-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Returns of expired drugs (HF and dependent wards)','ADJUSTMENT','CREDIT'),
('44814bc4-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Receive','TRANSFER','CREDIT'),
('44814cc8-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Donations to Deposit','ADJUSTMENT','CREDIT'),
('44814e30-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Returns from Quarantine, in the case of quarantined product \nbeing fit for use','ADJUSTMENT','CREDIT'),
('44814f2a-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Loans made from a health facility deposit','ADJUSTMENT','DEBIT'),
('44814fde-df64-11e9-9e7e-4c32759554d9',NULL,TRUE,'Negative Correction','ADJUSTMENT','DEBIT'),
('44815088-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Damaged On Arrival','ADJUSTMENT','DEBIT'),
('4481515a-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Drugs in quarantine have expired, returned to Supplier','ADJUSTMENT','DEBIT'),
('44815222-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Product defective, moved to quarantine','ADJUSTMENT','DEBIT'),
('448152fe-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Return to DDM','ADJUSTMENT','DEBIT'),
('448153bc-df64-11e9-9e7e-4c32759554d9',NULL,FALSE,'Issue','TRANSFER','DEBIT');