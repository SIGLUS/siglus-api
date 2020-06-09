-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE usage_columns_maps (
    id UUID PRIMARY KEY,
    requisitiontemplateid UUID NOT NULL,
    requisitioncolumnid UUID,
    definition text,
    displayorder INTEGER NOT NULL,
    indicator CHARACTER VARYING(255),
    isdisplayed boolean,
    label CHARACTER VARYING(255),
    name CHARACTER VARYING(255),
    source CHARACTER VARYING(255),
    key CHARACTER VARYING(255) NOT NULL,
    usagesectionid UUID NOT NULL,
    FOREIGN KEY(requisitioncolumnid) REFERENCES siglusintegration.available_usage_columns(id),
    FOREIGN KEY(usagesectionid) REFERENCES siglusintegration.usage_sections_maps(id)
);