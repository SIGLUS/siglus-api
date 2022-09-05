-- initial data for test

-- localmachine
CREATE SCHEMA IF NOT EXISTS localmachine;
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
CREATE UNIQUE INDEX IF NOT EXISTS localmachine_sender_localsequencenumber on localmachine.events(senderid, localsequencenumber);
CREATE UNIQUE INDEX IF NOT EXISTS localmachine_group_seq on localmachine.events(groupid, groupsequencenumber);

CREATE TABLE IF NOT EXISTS localmachine.agents (
    machineid UUID NOT NULL,
    facilityid UUID NOT NULL,
    facilitycode VARCHAR(255) NULL,
    privatekey TEXT NULL,
    publickey TEXT NULL,
    activationcode TEXT NULL,
    activatedat TIMESTAMP WITH TIME ZONE
);
CREATE TABLE IF NOT EXISTS localmachine.machine (
    touched  BOOL NOT NULL DEFAULT TRUE,
    id UUID NOT NULL PRIMARY KEY
);
