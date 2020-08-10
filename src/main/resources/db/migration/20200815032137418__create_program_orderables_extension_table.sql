-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.program_orderables_extension (
    id uuid PRIMARY KEY,
    programcode character varying(255),
    programname character varying(255),
    orderableid uuid,
	additionalorderableid uuid,
    realprogramcode character varying(255),
    realprogramname character varying(255)
);

CREATE INDEX index_orderableid ON siglusintegration.program_orderables_extension(orderableid uuid_ops);
CREATE INDEX index_additionalorderableid ON siglusintegration.program_orderables_extension(additionalorderableid uuid_ops);
