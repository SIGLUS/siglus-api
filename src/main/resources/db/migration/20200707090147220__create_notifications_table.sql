-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.notifications
(
    id               uuid PRIMARY KEY,
    refid            UUID                     NOT NULL,
    refstatus        VARCHAR(20)              NOT NULL,
    reffacilityid    UUID                     NOT NULL,
    refprogramid     UUID                     NOT NULL,
    notifyfacilityid UUID                     NOT NULL,
    viewed           BOOLEAN,
    processed        BOOLEAN,
    viewedUserId     UUID,
    vieweddate       TIMESTAMP WITH TIME ZONE,
    operatorId       UUID                     NOT NULL,
    createdate       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON siglusintegration.notifications (createdate);