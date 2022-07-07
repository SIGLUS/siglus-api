-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.notifications (
    id UUID PRIMARY KEY,
    refid UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    facilityid UUID NOT NULL,
    programid UUID NOT NULL,
    notifyfacilityid UUID,
    notifysupervisorynodeid UUID,
    requestingfacilityid UUID NOT NULL,
    processingPeriodId UUID NOT NULL,
    emergency BOOLEAN NOT NULL,
    type VARCHAR(20) NOT NULL,
    viewed BOOLEAN,
    processed BOOLEAN,
    viewedUserId UUID,
    vieweddate TIMESTAMP WITH TIME ZONE,
    operatorId UUID NOT NULL,
    createddate TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON siglusintegration.notifications (createddate);