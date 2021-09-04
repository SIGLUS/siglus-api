-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


CREATE TABLE requisition_line_items_draft (
    id uuid PRIMARY KEY NOT NULL,
    requisitiondraftid uuid NOT NULL,
    requisitionlineitemid uuid,
    adjustedconsumption integer,
    approvedquantity integer,
    averageconsumption integer,
    beginningbalance integer,
    calculatedorderquantity integer,
    maxperiodsofstock numeric(19,2),
    maximumstockquantity integer,
    nonfullsupply boolean,
    numberofnewpatientsadded integer,
    orderableid uuid,
    packstoship bigint,
    priceperpack numeric(19,2),
    remarks character varying(250),
    requestedquantity integer,
    requestedquantityexplanation character varying(255),
    skipped boolean,
    stockonhand integer,
    total integer,
    totalconsumedquantity integer,
    totalcost numeric(19,2),
    totallossesandadjustments integer,
    totalreceivedquantity integer,
    totalstockoutdays integer,
    requisitionid uuid,
    idealStockAmount integer,
    calculatedOrderQuantityIsa integer,
    additionalQuantityRequired integer,
    orderableversionnumber bigint,
    facilitytypeapprovedproductid uuid,
    facilitytypeapprovedproductversionnumber bigint,
    authorizedQuantity integer,
    FOREIGN KEY(requisitiondraftid) REFERENCES siglusintegration.requisitions_draft(id)
);