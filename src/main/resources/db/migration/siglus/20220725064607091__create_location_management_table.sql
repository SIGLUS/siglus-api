-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create table siglusintegration.location_management
(
    id           uuid        not null,
    facilityid   uuid        not null,
    locationcode varchar(8)  not null,
    area         varchar(32) not null,
    zone         varchar(4)  not null,
    rack         varchar(4)  not null,
    barcode      varchar(8)  not null,
    bin          int         not null,
    level        varchar(4)  not null
);

create unique index location_management_id_uindex
    on siglusintegration.location_management (id);

alter table siglusintegration.location_management
    add constraint location_management_pk
        primary key (id);