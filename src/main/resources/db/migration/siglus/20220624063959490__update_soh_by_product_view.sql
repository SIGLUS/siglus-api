drop view if exists dashboard.vw_stock_on_hand_by_product;
create view dashboard.vw_stock_on_hand_by_product as select
                                                         distinct on
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
        soh.totalsoh * po.priceperpack as totalprice,
        h.cmm as realcmm,
        case
        when h.cmm is null
        or h.cmm <= 0::double precision then 0::numeric
        else round(h.cmm::numeric, 2)
end as cmm,
	case
		when h.cmm is null
		or h.cmm <= 0::double precision then 0::numeric
		else round(soh.totalsoh::numeric / round(h.cmm::numeric, 2), 1)
end as mos,
	case
		when soh.totalsoh = 0 then 'Stockout'::text
		when h.cmm = '-1'::integer::double precision
		or h.cmm is null then 'Regular Stock'::text
		when soh.totalsoh::double precision < h.cmm then 'Low Stock'::text
		when prnm.programcode::text = 'TARV'::text
		and (h.cmm * 3::double precision) < soh.totalsoh::double precision then 'Overstock'::text
		when prnm.programcode::text <> 'TARV'::text
		and (h.cmm * 2::double precision) < soh.totalsoh::double precision then 'Overstock'::text
		else 'Regular Stock'::text
end as stockstatus,
	csoh.expirationdate as earlistdrugexpriydate,
	csoh.processeddate as lastupdatetime
from
	(
	select
		soh.facilityid,
		soh.orderableid,
		sum(soh.stockonhand) as totalsoh
	from
		(
		select
			sc.facilityid ,
			sc.orderableid ,
			csoh.stockonhand ,
			csoh .occurreddate ,
			row_number () over (partition by sc.id
		order by
			csoh.occurreddate desc)
		from
			stockmanagement.calculated_stocks_on_hand csoh
		left join stockmanagement.stock_cards sc on
			sc.id = csoh .stockcardid) soh
	where
		soh.row_number = 1
	group by
		soh.facilityid,
		soh.orderableid) soh
left join (
	select
		sc.facilityid ,
		sc.orderableid ,
		min(l.expirationdate) as expirationdate,
		max(csoh.processeddate) as processeddate
	from
		stockmanagement.stock_cards sc
	left join referencedata.lots l on
		sc.lotid = l.id
	left join stockmanagement.calculated_stocks_on_hand csoh on
		csoh.stockcardid = sc.id
	group by
		sc.facilityid,
		sc.orderableid) csoh on
	csoh.facilityid = soh.facilityid
	and csoh.orderableid = soh.orderableid
left join stockmanagement.stock_cards sc on sc.facilityid=soh.facilityid and sc.orderableid=soh.orderableid
left join siglusintegration.program_requisition_name_mapping prnm on prnm.programid =sc.programid
left join referencedata.facilities f on
	f.id = soh.facilityid
left join (
	select
		distinct on
		(id) * ,
		max(versionnumber) over (partition by id)
	from
		referencedata.orderables) o on
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
		distinct hc.facilitycode,
		hc.productcode,
		max(hc.cmm) over (partition by hc.facilitycode,
		hc.productcode) as cmm
	from
		siglusintegration.hf_cmms hc) h on
	h.facilitycode::text = f.code
	and h.productcode::text = o.code::text
left join referencedata.program_orderables po on
	po.orderableid = soh.orderableid;

