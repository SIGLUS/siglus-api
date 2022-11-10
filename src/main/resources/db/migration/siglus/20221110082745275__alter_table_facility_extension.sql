-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

update siglusintegration.facility_extension set enablelocationmanagement = false where enablelocationmanagement is null;

alter table siglusintegration.facility_extension
    alter column enablelocationmanagement set default false,
    alter column enablelocationmanagement set not null;