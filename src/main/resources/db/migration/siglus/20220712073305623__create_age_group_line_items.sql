-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE if not exists "siglusintegration"."age_group_line_items" (
    "id" uuid,
    "requisitionid" uuid NOT NULL,
    "groupname" character varying(255),
    "columnname" character varying(255),
    "value" integer,
    PRIMARY KEY ("id")
    );
CREATE INDEX if not exists "age_group_line_items_requisitionid_idx" ON "siglusintegration"."age_group_line_items"("requisitionid");
