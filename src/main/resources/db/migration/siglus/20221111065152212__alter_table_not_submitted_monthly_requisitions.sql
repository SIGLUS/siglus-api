-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

alter table dashboard.requisition_monthly_not_submit_report rename to not_submitted_monthly_requisitions;
alter table dashboard.not_submitted_monthly_requisitions add column processingperiodname text;

