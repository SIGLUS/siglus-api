-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


CREATE TABLE usage_sections_maps (
    id UUID PRIMARY KEY,
    name CHARACTER VARYING(255) NOT NULL,
    label CHARACTER VARYING(255),
    displayorder INTEGER NOT NULL,
    sectionid UUID,
    category CHARACTER VARYING(255),
    FOREIGN KEY(sectionid) REFERENCES siglusintegration.available_usage_column_sections(id)
);