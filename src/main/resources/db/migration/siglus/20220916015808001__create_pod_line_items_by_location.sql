-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create table pod_line_items_by_location
(
    id               uuid         not null,
    podlineitemid    uuid         not null,
    locationcode     varchar(255) not null,
    area             varchar(255) not null,
    quantityaccepted integer      not null
);

create unique index pod_line_items_by_location_id_uindex
    on pod_line_items_by_location (id);

create index pod_line_items_by_location_podlineitemid_index
    on pod_line_items_by_location (podlineitemid);