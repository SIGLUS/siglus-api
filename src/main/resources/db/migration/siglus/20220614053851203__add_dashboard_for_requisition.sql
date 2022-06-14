-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO siglusintegration.metabase_config (id, dashboardid, dashboardname) VALUES ('2763e3cd-0310-4e58-b705-1187a5810563', 27, 'requisition_monthly_report');