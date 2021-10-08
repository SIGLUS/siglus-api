-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP TABLE siglusintegration.fc_integration_results;

CREATE TABLE siglusintegration.fc_integration_results (
    id uuid PRIMARY KEY,
    api character varying(255),
    startdate character varying(255),
    enddate character varying(255),
    lastupdatedat timestamp with time zone,
    processdate timestamp with time zone,
    totalobjects integer,
    createdobjects integer,
    updatedobjects integer,
    finalsuccess boolean,
    errormessage text
);

CREATE INDEX ON siglusintegration.fc_integration_results(api text_ops, lastupdatedat timestamptz_ops);
