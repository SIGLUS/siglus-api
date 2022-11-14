-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP VIEW if EXISTS dashboard.vw_stock_status;
ALTER VIEW dashboard.vw_stock_on_hand_by_product RENAME TO vw_stock_status;
ALTER VIEW dashboard.vw_mmia_regimens_report RENAME TO vw_mmia_regimens;
ALTER VIEW dashboard.vw_mmtb_regimens_report RENAME TO vw_mmtb_regimens;
ALTER VIEW dashboard.vw_system_update_report RENAME TO vw_system_update;