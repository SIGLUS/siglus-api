-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_stock_on_hand_by_product;
create view dashboard.vw_stock_on_hand_by_product as
select
    distinct on (f.code,o.code)
             f.code as facilitycode,
             f.name as facilityname,
             ft.code as facilitytype,
             o.code as productcode,
             o.fullproductname as productname,
             gz_prov.code as provincecode,
             gz_prov.name as provincename,
             gz.code as districtcode,
             gz.name as districtname,
             vfs.districtfacilitycode,
             vfs.provincefacilitycode,
             csoh.stockonhand as totalsoh,
             csoh.stockonhand::numeric * po.priceperpack as totalprice,
        h.cmm as realcmm,
             case
                 when h.cmm is null
                     or h.cmm <= 0::double precision then 0::numeric
		else round(h.cmm::numeric, 2)
end as cmm,
	case
		when h.cmm is null
		or h.cmm <= 0::double precision then 0::numeric
		else round(csoh.stockonhand::numeric / round(h.cmm::numeric, 2), 1)
end as mos,
	case
		when h.cmm = '-1'::integer::double precision
		or h.cmm is null then 'Regular Stock'::text
		when csoh.stockonhand = 0 then 'Stockout'::text
		when csoh.stockonhand::double precision < h.cmm then 'Low Stock'::text
		when ((o.extradata::json -> 'isHiv'::text)::text) = 'true'::text
		and (h.cmm * 3::double precision) < csoh.stockonhand::double precision then 'Overstock'::text
		when ((o.extradata::json -> 'isHiv'::text)::text) <> 'true'::text
		or ((o.extradata::json -> 'isHiv'::text)::text) is null
		and (h.cmm * 2::double precision) < csoh.stockonhand::double precision then 'Overstock'::text
		else 'Regular Stock'::text
end as stockstatus,
	first_value(l.expirationdate) over (partition by sc.orderableid
order by
	l.expirationdate) as earlistdrugexpriydate,
	csoh.processeddate as lastupdatetime
from
	referencedata.facilities f
left join stockmanagement.stock_cards sc on
	f.id = sc.facilityid
left join referencedata.lots l on
	l.id = sc.lotid
left join (
	select
		calculated_stocks_on_hand.stockcardid,
		calculated_stocks_on_hand.stockonhand,
		first_value(calculated_stocks_on_hand.processeddate) over (partition by calculated_stocks_on_hand.stockcardid
	order by
		calculated_stocks_on_hand.processeddate desc) as processeddate
	from
		stockmanagement.calculated_stocks_on_hand) csoh on
	csoh.stockcardid = sc.id
left join (
	select
		distinct on
		(orderables.id) orderables.id,
		orderables.fullproductname,
		orderables.extradata,
		first_value(orderables.code) over (partition by orderables.id
	order by
		orderables.versionnumber desc) as code
	from
		referencedata.orderables) o on
	o.id = sc.orderableid
left join referencedata.program_orderables po on
	po.orderableid = o.id
left join (
	select
		distinct hc.facilitycode,
		hc.productcode,
		first_value(hc.cmm) over (partition by hc.facilitycode,
		hc.productcode
	order by
		hc.periodend desc) as cmm
	from
		siglusintegration.hf_cmms hc) h on
	h.facilitycode::text = f.code
	and h.productcode::text = o.code::text
left join referencedata.geographic_zones gz on
	gz.id = f.geographiczoneid
left join referencedata.geographic_zones gz_prov on
	gz_prov.id = gz.parentid
left join dashboard.vw_facility_supplier vfs on
	vfs.facilitycode = f.code
left join referencedata.facility_types ft on
	ft.id = f.typeid
where
	o.code is not null;

