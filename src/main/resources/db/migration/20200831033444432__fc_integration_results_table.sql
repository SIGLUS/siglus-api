-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.fc_integration_results (
    id uuid PRIMARY KEY,
    job CHARACTER VARYING(255),
    startdate CHARACTER VARYING(255),
    enddate CHARACTER VARYING(255),
    finishtime TIMESTAMP WITH TIME ZONE,
    callfcsuccess boolean,
    callfccosttimeinseconds INTEGER,
    finalsuccess boolean,
    totalcosttimeinseconds INTEGER
);

CREATE INDEX ON siglusintegration.fc_integration_results(job);
CREATE INDEX ON siglusintegration.fc_integration_results(startdate);
CREATE INDEX ON siglusintegration.fc_integration_results(enddate);
