-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create table siglusintegration.physical_inventories_extension
(
    id                  uuid        not null
        constraint physical_inventories_extension_pk
            primary key,
    physicalinventoryid uuid        not null,
    category            varchar(16) not null
);