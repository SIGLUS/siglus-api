-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop table if exists pod_line_items_by_location;

create table siglusintegration.pod_line_items_by_location
(
    id               uuid         not null,
    podlineitemid    uuid         not null,
    locationcode     varchar(255) not null,
    area             varchar(255) not null,
    quantityaccepted integer      not null
);

create unique index pod_line_items_by_location_id_uindex
    on siglusintegration.pod_line_items_by_location (id);

create index pod_line_items_by_location_podlineitemid_index
    on siglusintegration.pod_line_items_by_location (podlineitemid);

create table siglusintegration.pod_sub_draft_line_items_by_location
(
    id               uuid not null,
    podlineitemid    uuid not null,
    locationcode     varchar(255),
    area             varchar(255),
    quantityaccepted integer
);

create unique index pod_sub_draft_line_items_by_location_id_uindex
    on siglusintegration.pod_sub_draft_line_items_by_location (id);

create index pod_sub_draft_line_items_by_location_podlineitemid_index
    on siglusintegration.pod_sub_draft_line_items_by_location (podlineitemid);