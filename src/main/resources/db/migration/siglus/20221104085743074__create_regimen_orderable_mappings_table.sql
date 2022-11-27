-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE
UNIQUE INDEX IF NOT EXISTS "regimen_code_idx" ON "siglusintegration"."regimens"("code");

CREATE TABLE siglusintegration.regimen_orderable_mappings
(
    id             uuid PRIMARY KEY,
    regimencode    CHARACTER VARYING(255) NOT NULL
        constraint regimen_orderable_mappings_regimens_code_fk
            references regimens (code),
    orderablecode  CHARACTER VARYING(255) NOT NULL,
    quantity       DECIMAL                NOT NULL,
    programcode    CHARACTER VARYING(255) NOT NULL,
    createdTime    TIMESTAMP WITH TIME ZONE,
    lastupdatetime TIMESTAMP WITH TIME ZONE
);

CREATE
INDEX index_regimencode ON siglusintegration.regimen_orderable_mappings(regimencode text_ops);
CREATE
UNIQUE INDEX unq_regimencode_orderablecode ON siglusintegration.regimen_orderable_mappings(regimencode text_ops,orderablecode text_ops);
