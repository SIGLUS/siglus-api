-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.fc_integration_changes
(
    id            uuid PRIMARY KEY,
    resultid      uuid NOT NULL,
    type          character varying(20),
    category      character varying(50),
    code          text NOT NULL,
    origincontent text,
    fccontent     text,
    updatetime    timestamp with time zone DEFAULT now()
);