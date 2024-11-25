-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- update table: pod_sub_draft_line_items
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD quantityrejected int4 NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD vvmstatus varchar(255) NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD usevvm boolean DEFAULT false NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD rejectionreasonid uuid NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD orderableversionnumber int8 NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD notes text NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD lotcode varchar(255) NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD expirationdate date NULL;
-- ALTER TABLE siglusintegration.pod_sub_draft_line_items ADD locations jsonb NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='quantityrejected'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN quantityrejected int4;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='vvmstatus'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN vvmstatus varchar(255);
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='usevvm'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN usevvm boolean;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='rejectionreasonid'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN rejectionreasonid uuid;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='orderableversionnumber'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN orderableversionnumber int8;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='notes'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN notes text;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='lotcode'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN lotcode varchar(255);
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='expirationdate'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN expirationdate date;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='siglusintegration' AND table_name='pod_sub_draft_line_items' AND column_name='locations'
    ) THEN
ALTER TABLE siglusintegration.pod_sub_draft_line_items
    ADD COLUMN locations jsonb;
END IF;
END $$;

CREATE INDEX IF NOT EXISTS pod_sub_draft_line_items_podsubdraftid_idx ON siglusintegration.pod_sub_draft_line_items (podsubdraftid);