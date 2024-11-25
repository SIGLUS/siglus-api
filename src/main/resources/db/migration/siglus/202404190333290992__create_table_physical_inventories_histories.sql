-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE IF NOT EXISTS siglusintegration.physical_inventories_histories
(
    id            uuid PRIMARY KEY,
    facilityid    uuid                     NOT NULL,
    historydata   jsonb,
    programid     uuid                     NOT NULL,
    completeddate date                     NOT NULL,
    processdate   timestamp with time zone NOT NULL
);

CREATE UNIQUE INDEX physical_inventories_histories_pkey ON siglusintegration.physical_inventories_histories (id uuid_ops);
CREATE INDEX idx_facilityid ON siglusintegration.physical_inventories_histories (facilityid uuid_ops);