-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_stock_status;
create view dashboard.vw_stock_status as
select distinct
        on
        (soh.facilityid,
        soh.orderableid) soh.facilityid,
        f.name as facilityname,
        f.code as facilitycode,
        ft.code as facilitytype,
        soh.orderableid,
        o.code as productcode,
        o.fullproductname as productname,
        gz_prov.name as provincename,
        gz.name as districtname,
        vfs.districtfacilitycode,
        vfs.provincefacilitycode,
        soh.totalsoh,
        soh.totalsoh:: numeric * po.priceperpack as totalprice,
        h.cmm as realcmm,
        case
        when h.cmm is null
        or h.cmm <= 0:: double precision then 0:: numeric
        else round(h.cmm:: numeric, 2)
end
as cmm,
	case
		when h.cmm is null
		or h.cmm <= 0::double precision then 0::numeric
		else round(soh.totalsoh::numeric / round(h.cmm::numeric, 2), 1)
end
as mos,
	case
		when soh.totalsoh = 0 then 'Roptura de stock'::text
		when h.cmm = '-1'::integer::double precision
		or h.cmm is null then 'Stock regular'::text
		when soh.totalsoh::double precision < h.cmm then 'Eminência de roptura'::text
		when prnm.programcode::text = 'TARV'::text
		and (h.cmm * 3::double precision) < soh.totalsoh::double precision then 'Stock acumulado'::text
		when prnm.programcode::text <> 'TARV'::text
		and (h.cmm * 2::double precision) < soh.totalsoh::double precision then 'Stock acumulado'::text
		else 'Stock regular'::text
end
as stockstatus,
	csoh.expirationdate as earlistdrugexpriydate,
	csoh.processeddate as lastupdatetime
from
	(
	select
		soh_1.facilityid,
		soh_1.orderableid,
		sum(soh_1.stockonhand) as totalsoh
	from
		(
		select
			sc_1.facilityid,
			sc_1.orderableid,
			csoh_1.stockonhand,
			csoh_1.occurreddate,
			row_number() over (partition by sc_1.id
		order by
			csoh_1.occurreddate desc) as row_number
		from
			stockmanagement.calculated_stocks_on_hand csoh_1
		left join stockmanagement.stock_cards sc_1 on
			sc_1.id = csoh_1.stockcardid) soh_1
	where
		soh_1.row_number = 1
	group by
		soh_1.facilityid,
		soh_1.orderableid) soh
left join (
	select
		sc_1.facilityid,
		sc_1.orderableid,
		min(l.expirationdate) as expirationdate,
		max(csoh_1.processeddate) as processeddate
	from
		stockmanagement.stock_cards sc_1
	left join referencedata.lots l on
		sc_1.lotid = l.id
	left join stockmanagement.calculated_stocks_on_hand csoh_1 on
		csoh_1.stockcardid = sc_1.id
	group by
		sc_1.facilityid,
		sc_1.orderableid) csoh on
	csoh.facilityid = soh.facilityid
	and csoh.orderableid = soh.orderableid
left join stockmanagement.stock_cards sc on
	sc.facilityid = soh.facilityid
	and sc.orderableid = soh.orderableid
left join siglusintegration.program_requisition_name_mapping prnm on
	prnm.programid = sc.programid
left join referencedata.facilities f on
	f.id = soh.facilityid
left join (
	select
		*
	from
		(
		select
			id,
			code,
			fullproductname,
			versionnumber,
			max(versionnumber) over(partition by id) latestversion
		from
			referencedata.orderables) o
	where
		o.versionnumber = o.latestversion) o on
	o.id = soh.orderableid
left join referencedata.facility_types ft on
	f.typeid = ft.id
left join referencedata.geographic_zones gz on
	gz.id = f.geographiczoneid
left join referencedata.geographic_zones gz_prov on
	gz_prov.id = gz.parentid
left join dashboard.vw_facility_supplier vfs on
	vfs.facilitycode = f.code
left join (
	select
		distinct on (hc.facilitycode,hc.productcode)
        hc.facilitycode,
		hc.productcode,
		first_value(hc.cmm) over (partition by hc.facilitycode,
		hc.productcode order by hc.periodend desc) as cmm
	from
		siglusintegration.hf_cmms hc) h on
	h.facilitycode::text = f.code
	and h.productcode::text = o.code::text
left join referencedata.program_orderables po on
	po.orderableid = soh.orderableid;

