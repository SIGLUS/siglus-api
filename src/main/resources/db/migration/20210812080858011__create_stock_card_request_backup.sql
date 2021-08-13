-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.stock_card_request_backup
(
    id            UUID PRIMARY KEY,
    hash          CHARACTER VARYING(255)   NOT NULL UNIQUE,
    facilityid    UUID                     NOT NULL,
    userid        UUID                     NOT NULL,
    programcode   CHARACTER VARYING(255)   ,
    processedDate TIMESTAMP WITH TIME ZONE ,
    stockOnHand   INTEGER                  ,
    quantity      INTEGER                  ,
    type          CHARACTER VARYING(255)   ,
    errormessage  TEXT,
    requestbody   jsonb,
    createddate   TIMESTAMP WITH TIME ZONE
);
