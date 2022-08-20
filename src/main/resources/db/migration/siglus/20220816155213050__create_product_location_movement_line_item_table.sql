-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.product_location_movement_line_items
(
    id               UUID PRIMARY KEY NOT NULL,
    stockcardid      UUID             NOT NULL,
    createddate      date             NOT NULL,
    signature        VARCHAR(255)     NOT NULL,
    userid           UUID             NOT NULL,
    srcarea          VARCHAR(255)     NOT NULL,
    srclocationcode  VARCHAR(255)     NOT NULL,
    destarea         VARCHAR(255)     NOT NULL,
    destlocationcode VARCHAR(255)     NOT NULL,
    quantity         INTEGER          NOT NULL
);

ALTER TABLE siglusintegration.product_location_movement_draft_line_items DROP COLUMN srclocationid;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items DROP COLUMN destlocationid;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items ADD COLUMN srcarea VARCHAR(255) NOT NULL;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items ADD COLUMN destarea VARCHAR(255) NOT NULL;