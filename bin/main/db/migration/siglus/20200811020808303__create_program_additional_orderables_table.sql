-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.program_additional_orderables (
    id uuid PRIMARY KEY,
    programid uuid,
    additionalorderableid uuid,
    orderableoriginprogramid uuid
);

CREATE INDEX index_programid ON siglusintegration.program_additional_orderables(programid uuid_ops);
CREATE UNIQUE INDEX unq_programid_additionalorderableid ON siglusintegration.program_additional_orderables(programid uuid_ops,additionalorderableid uuid_ops);
