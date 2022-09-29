-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create table siglusintegration.physical_inventory_empty_location_line_items
(
    id           uuid not null
        constraint physical_inventory_empty_location_line_items_pk
            primary key,
    subdraftid   uuid not null,
    skipped      boolean default false,
    locationcode varchar(255),
    area         varchar(255),
    hasproduct   boolean default false
);

alter table siglusintegration.physical_inventory_empty_location_line_items
    owner to postgres;

create unique index physical_inventory_empty_location_line_items_id_uindex
    on siglusintegration.physical_inventory_empty_location_line_items (id);