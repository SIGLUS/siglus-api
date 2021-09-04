-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


CREATE TABLE available_usage_columns (
    id UUID PRIMARY KEY,
    canbechangedbyuser BOOLEAN,
    canchangeorder BOOLEAN,
    columntype CHARACTER VARYING(255),
    definition TEXT,
    indicator CHARACTER VARYING(255),
    isdisplayrequired BOOLEAN,
    label CHARACTER VARYING(255),
    mandatory BOOLEAN,
    name CHARACTER VARYING(255),
    supportsTag BOOLEAN DEFAULT false,
    sectionid UUID NOT NULL,
    sources CHARACTER VARYING(255),
    displayorder INTEGER NOT NULL,
    FOREIGN KEY(sectionid) REFERENCES siglusintegration.available_usage_column_sections(id)
);