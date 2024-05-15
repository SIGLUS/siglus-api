-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP TABLE IF EXISTS siglusintegration.physical_inventories_histories;
CREATE TABLE siglusintegration.physical_inventories_histories
(
    id                              uuid PRIMARY KEY,
    facilityid                      uuid NOT NULL,
    physicalinventoryextensionid    uuid NOT NULL,
    groupid                         uuid NOT NULL
);


CREATE INDEX idx_facilityid
    ON siglusintegration.physical_inventories_histories
    (facilityid uuid_ops);

CREATE INDEX idx_groupid
    ON siglusintegration.physical_inventories_histories
    (groupid uuid_ops);

CREATE UNIQUE INDEX unq_physicalinventoryextensionid
    ON siglusintegration.physical_inventories_histories
    (physicalinventoryextensionid uuid_ops);

CREATE UNIQUE INDEX unq_facilityid_physicalinventoryextensionid_groupid
    ON siglusintegration.physical_inventories_histories
    (facilityid uuid_ops, physicalinventoryextensionid uuid_ops, groupid uuid_ops);
