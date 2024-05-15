-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_fefo;
create view dashboard.vw_fefo as
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
t3.totalordercount,
case
    when t4.fefoordercount is null then 0
    else t4.fefoordercount
end as fefoordercount,
case
    when t3.totalordercount = 0 then 0
    when fefoordercount is null then 0
    else cast(fefoordercount as float)/cast(t3.totalordercount as float)
end as fefopercent
from
(
	select t1.id, count(*) as totalOrderCount
	from
	(
		select f.id, se.isfefo
		from referencedata.facilities f
		left join fulfillment.orders o on f.id = o.supplyingfacilityid
		left join fulfillment.shipments s on o.id = s.orderid
		left join siglusintegration.shipments_extension se on s.id = se.shipmentid
		where
		se.isfefo is not null
	) t1
	group by t1.id
) t3
left join
(
	select t2.id, count(*) as fefoOrderCount
	from
	(
		select f.id, se.isfefo
		from referencedata.facilities f
		left join fulfillment.orders o on f.id = o.supplyingfacilityid
		left join fulfillment.shipments s on o.id = s.orderid
		left join siglusintegration.shipments_extension se on s.id = se.shipmentid
		where
		se.isfefo = 'true'
	) t2
	group by t2.id
) t4 on t3.id = t4.id
left join referencedata.facilities f on t3.id = f.id
left join referencedata.facility_types ft on f.typeid = ft.id
left join siglusintegration.facility_type_mapping ftm on ft.code = ftm.facilitytypecode
left join dashboard.vw_facility_supplier vfs on f.code = vfs.facilitycode
left join referencedata.geographic_zones gz on f.geographiczoneid = gz.id
left join referencedata.geographic_zones pgz on gz.parentid = pgz.id;
