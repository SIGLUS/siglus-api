-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.requisition_template_extension (
    id uuid PRIMARY KEY,
    requisitiontemplateid uuid NOT NULL UNIQUE,
    enableconsultationnumber boolean DEFAULT false,
    enablekitusage boolean DEFAULT false,
    enableproduct boolean DEFAULT false,
    enablepatientlineitem boolean DEFAULT false,
    enableregimen boolean DEFAULT false,
    enablerapidtestconsumption boolean DEFAULT false,
    enableusageinformation boolean DEFAULT false
);
