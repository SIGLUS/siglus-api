-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
update stockmanagement.physical_inventories
set isdraft = false
where id in (
    select pi.id
    from stockmanagement.physical_inventories pi
    left join siglusintegration.physical_inventories_extension pie on pi.id = pie.physicalinventoryid
    where pi.isdraft = true and pie.category is null
);
