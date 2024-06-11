-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- update table: pod_sub_draft_line_items
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD quantityrejected int4 NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD vvmstatus varchar(255) NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD usevvm boolean DEFAULT false NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD rejectionreasonid int4 NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD orderableversionnumber int8 NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD notes text NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD lotcode varchar(255) NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD expirationdate date NULL;
ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD locations jsonb NULL;
