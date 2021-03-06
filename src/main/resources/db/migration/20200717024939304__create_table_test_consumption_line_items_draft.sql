-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


CREATE TABLE siglusintegration.test_consumption_line_items_draft (
  id UUID PRIMARY KEY,
  testconsumptionlineitemid UUID NOT NULL,
  requisitionid UUID NOT NULL,
  service CHARACTER VARYING(255),
  project CHARACTER VARYING(255),
  outcome CHARACTER VARYING(255),
  value INTEGER,
  requisitiondraftid uuid NOT NULL,
  FOREIGN KEY(requisitiondraftid) REFERENCES siglusintegration.requisitions_draft(id)
);
