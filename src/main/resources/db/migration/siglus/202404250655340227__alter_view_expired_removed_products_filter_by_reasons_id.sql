-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_expired_removed_products;
create view dashboard.vw_expired_removed_products as
select
case
    when pgz.name is null then gz.name
    else pgz.name
end as provincename,
vfs.provincefacilitycode,
gz.name as districtname,
vfs.districtfacilitycode,
f.name as facilityname,
f.code as facilitycode,
p.name as programname,
o.code as productcode,
o.fullproductname as productname,
l.lotcode,
l.expirationdate,
scli.quantity as amountremoved,
scli.occurreddate as removeddate
from stockmanagement.stock_cards sc
left join referencedata.facilities f on sc.facilityid = f.id
left join dashboard.vw_facility_supplier vfs on f.code = vfs.facilitycode
left join referencedata.geographic_zones gz on f.geographiczoneid = gz.id
left join referencedata.geographic_zones pgz on gz.parentid = pgz.id
left join referencedata.programs p on sc.programid = p.id
left join
(
	select distinct on(id) id, fullproductname, code, versionnumber
	from referencedata.orderables
	order by id, versionnumber desc
) o on sc.orderableid = o.id
left join referencedata.lots l on sc.lotid = l.id
left join stockmanagement.stock_card_line_items scli on sc.id = scli.stockcardid
where scli.reasonid = '23f7d0d4-02ce-11ef-9071-12c51f50079a';
