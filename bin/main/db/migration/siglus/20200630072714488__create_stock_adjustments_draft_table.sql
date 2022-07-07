-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE stock_adjustments_draft (
    id uuid PRIMARY KEY NOT NULL,
    quantity integer NOT NULL,
    reasonid uuid NOT NULL,
    requisitionlineitemid uuid,
    FOREIGN KEY(requisitionlineitemid) REFERENCES siglusintegration.requisition_line_items_draft(id)
);