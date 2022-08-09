-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_stock_on_hand_by_lot;
create view dashboard.vw_stock_on_hand_by_lot as
select distinct
        on
        (sc.id) f.code as facilitycode,
        f.name as facilityname,
        ft.code as facilitytype,
        gz.code as districtcode,
        gz.name as districtname,
        gz_prov.code as provincecode,
        gz_prov.name as provincename,
        vfs.districtfacilitycode,
        vfs.provincefacilitycode,
        o.code as productcode,
        o.fullproductname as productname,
        l.id as lotid,
        l.lotcode as lotnumber,
        csoh.stockonhand as sohoflot,
        l.expirationdate as expriydate,
        csoh.processeddate as lastupdatefromtablet,
        hc.cmm as realcmm,
        case
        when hc.cmm is null
        or hc.cmm <= 0:: double precision then 0:: numeric
        else round(hc.cmm:: numeric, 2)
end
as cmm,
	case
		when hc.cmm is null
		or hc.cmm <= 0::double precision then 0::numeric
		else round(csoh.stockonhand::numeric / round(hc.cmm::numeric, 2), 1)
end
as mos
from
	(
	select
		*
	from
		stockmanagement.stock_cards
	where
		lotid is not null) sc
left join referencedata.lots l on
	l.id = sc.lotid
left join referencedata.facilities f on
	f.id = sc.facilityid
left join referencedata.geographic_zones gz on
	gz.id = f.geographiczoneid
left join referencedata.geographic_zones gz_prov on
	gz_prov.id = gz.parentid
left join (
	select
		distinct on
		(orderables.id) orderables.id,
		orderables.code,
		orderables.fullproductname,
		max(orderables.versionnumber) over (partition by orderables.id) as max
	from
		referencedata.orderables) o on
	o.id = sc.orderableid
left join (
	select
		*
	from
		(
		select
			stockcardid ,
			stockonhand,
			occurreddate,
			max(processeddate) over (partition by stockcardid) as processeddate,
			row_number () over (partition by stockcardid
		order by
			occurreddate desc)
		from
			stockmanagement.calculated_stocks_on_hand)cs
	where
		row_number = 1 ) csoh on
	csoh.stockcardid = sc.id
left join
    (
	select
		distinct on (hc.facilitycode,hc.productcode)
        hc.facilitycode,
		hc.productcode,
		first_value(hc.cmm) over (partition by hc.facilitycode,
		hc.productcode order by hc.periodend desc) as cmm
	from
		siglusintegration.hf_cmms hc) hc on
	hc.facilitycode::text = f.code
	and hc.productcode::text = o.code::text
left join dashboard.vw_facility_supplier vfs on
	vfs.facilitycode = f.code
left join referencedata.facility_types ft on
	f.typeid = ft.id;