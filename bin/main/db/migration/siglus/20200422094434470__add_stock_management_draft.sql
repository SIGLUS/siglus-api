CREATE TABLE siglusintegration.stock_management_drafts
(
    id UUID PRIMARY KEY NOT NULL,
    facilityid UUID NOT NULL,
    isdraft BOOLEAN NOT NULL,
    occurreddate DATE,
    programid UUID NOT NULL,
    signature VARCHAR(255),
    userid UUID,
    drafttype VARCHAR(20)
);
CREATE TABLE siglusintegration.stock_management_draft_line_items
(
    id UUID PRIMARY KEY NOT NULL,
    orderableid UUID NOT NULL,
    lotid UUID,
    expirationdate DATE,
    lotcode text ,
    documentnumber VARCHAR(255),
    quantity INTEGER,
    stockmanagementDraftId UUID NOT NULL,
    destinationid UUID,
    destinationfreetext VARCHAR(255),
    sourceid UUID,
    sourcefreetext VARCHAR(255),
    reasonid UUID,
    reasonfreetext VARCHAR(255),
    extradata jsonb,
    occurreddate date
);
ALTER TABLE siglusintegration.stock_management_draft_line_items ADD FOREIGN KEY (stockmanagementDraftId) REFERENCES stock_management_drafts (id);


create Index draftIndex on siglusintegration.stock_management_drafts(programid, userid, isdraft, drafttype);


