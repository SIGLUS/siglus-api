-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.receipt_plan_line_item (
    id uuid PRIMARY KEY,
    receiptplanid uuid NOT NULL,
    productcode CHARACTER VARYING(255),
    productname CHARACTER VARYING(255),
    approvedquantity INTEGER,
    FOREIGN KEY(receiptplanid) REFERENCES siglusintegration.receipt_plan(id)
)
