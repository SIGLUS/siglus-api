-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP TABLE IF EXISTS siglusintegration.report_access_record;
CREATE TABLE siglusintegration.report_access_record
(
    id                     uuid PRIMARY KEY,
    userid                 uuid NOT NULL,
    reportname             character varying(255) NOT NULL,
    accessdate             date NOT NULL
);

CREATE UNIQUE INDEX unq_userid_reportname_accessdate
    ON siglusintegration.report_access_record (userid uuid_ops, reportname text_ops, accessdate date_ops);
