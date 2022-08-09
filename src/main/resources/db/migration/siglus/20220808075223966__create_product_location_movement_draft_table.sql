-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE siglusintegration.product_location_movement_drafts
(
    id UUID PRIMARY KEY NOT NULL,
    facilityid UUID NOT NULL,
    programid UUID NOT NULL,
    createddate DATE,
    signature VARCHAR(255),
    userid UUID
);
CREATE TABLE siglusintegration.product_location_movement_draft_line_items
(
    id UUID PRIMARY KEY NOT NULL,
    orderableid UUID NOT NULL,
    productname text,
    productcode text,
    lotid UUID,
    lotcode text,
    locationid UUID,
    locationcode text,
    movetoid UUID,
    movetocode text,
    createddate date,
    expirationdate DATE,
    quantity INTEGER,
    stockonhand INTEGER,
    productlocationmovementdraftId UUID NOT NULL
);

ALTER TABLE siglusintegration.product_location_movement_draft_line_items ADD FOREIGN KEY (productlocationmovementdraftId) REFERENCES product_location_movement_drafts (id);

create Index product_location_movement_index on siglusintegration.product_location_movement_drafts(facilityid, programid, userid);


