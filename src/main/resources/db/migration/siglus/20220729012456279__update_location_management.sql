-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

alter table siglusintegration.location_management
alter column locationcode type varchar(255) using locationcode::varchar(255);

alter table siglusintegration.location_management
alter column area type varchar(255) using area::varchar(255);

alter table siglusintegration.location_management
alter column zone type varchar(255) using zone::varchar(255);

alter table siglusintegration.location_management
alter column rack type varchar(255) using rack::varchar(255);

alter table siglusintegration.location_management
alter column barcode type varchar(255) using barcode::varchar(255);

alter table siglusintegration.location_management
alter column level type varchar(255) using level::varchar(255);

create index location_management_facility_id_index
    on siglusintegration.location_management (facilityid);

create index location_management_facility_id_location_code_index
    on siglusintegration.location_management (facilityid, locationcode);