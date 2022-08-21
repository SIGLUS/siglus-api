-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE SEQUENCE IF NOT EXISTS localmachine.event_seq;
CREATE TABLE IF NOT EXISTS localmachine.events (
    id UUID NOT NULL PRIMARY KEY,
    localsequencenumber BIGINT NOT NULL DEFAULT nextval('localmachine.event_seq'),
    protocolversion INT NOT NULL,
    occurredtime TIMESTAMP DEFAULT now(),
    senderid UUID NOT NULL,
    receiverid UUID NULL,
    groupid VARCHAR(255) NULL,
    groupsequencenumber BIGINT NULL,
    payload TEXT NULL,
    onlinewebsynced BOOL NOT NULL DEFAULT FALSE,
    receiversynced BOOL NOT NULL DEFAULT FALSE,
    localreplayed BOOL NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX localmachine_sender_localsequencenumber on localmachine.events(senderid, localsequencenumber);
CREATE UNIQUE INDEX localmachine_group_seq on localmachine.events(groupid, groupsequencenumber);
