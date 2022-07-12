-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop table if exists siglusintegration.shedlock;
CREATE TABLE siglusintegration.shedlock
(
    name       character varying(64) PRIMARY KEY,
    lock_until timestamp(3) without time zone,
    locked_at  timestamp(3) without time zone,
    locked_by  character varying(255)
);
