-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_fefo_pie;
create view dashboard.vw_fefo_pie as
select
case
    when pgz.name is null then gz.name
    else pgz.name
end as provincename,
gz.name AS districtname,
vfs.provincefacilitycode,
vfs.districtfacilitycode,
f.code as facilitycode,
f.name as facilityname,
ft.code as facilitytype,
ftm.category as facilitymergetype,
se.isfefo
from referencedata.facilities f
left join fulfillment.orders o on f.id = o.supplyingfacilityid
left join fulfillment.shipments s on o.id = s.orderid
left join siglusintegration.shipments_extension se on s.id = se.shipmentid
left join referencedata.facility_types ft on f.typeid = ft.id
left join siglusintegration.facility_type_mapping ftm on ft.code = ftm.facilitytypecode
left join dashboard.vw_facility_supplier vfs on f.code = vfs.facilitycode
left join referencedata.geographic_zones gz on f.geographiczoneid = gz.id
left join referencedata.geographic_zones pgz on gz.parentid = pgz.id
where se.isfefo is not null;
