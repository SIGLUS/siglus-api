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
    parentid UUID NULL,
    archived boolean NOT NULL DEFAULT FALSE,
    onlinewebsynced INT NOT NULL DEFAULT 0,
    receiversynced INT NOT NULL DEFAULT 0,
    localreplayed INT NOT NULL DEFAULT 0,
    syncedtime TIMESTAMP DEFAULT now()
);

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
    touched  INT NOT NULL DEFAULT 1,
    id UUID NOT NULL PRIMARY KEY
);

CREATE TABLE localmachine.event_payload
(
    eventid uuid PRIMARY KEY,
    payload bytea
);

CREATE TABLE localmachine.event_payload_backup
(
    eventid uuid PRIMARY KEY,
    payload bytea
);

CREATE TABLE IF NOT EXISTS localmachine.ack_records (
    eventid uuid PRIMARY KEY,
    sendto uuid NOT NULL,
    shipped BOOL NOT NULL DEFAULT FALSE
);

alter table localmachine.events add column category varchar(511);