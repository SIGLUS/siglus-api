-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_historical_data;

drop table if exists hdm;
select * into hdm from (select
                            pp.id as periodid,
                            pp.startdate ,
                            pp.enddate ,
                            sc.facilityid ,
                            sc.orderableid ,
                            sc.lotid ,
                            csoh.occurreddate ,
                            csoh.stockonhand
                        from
                            referencedata.processing_periods pp
                                left join referencedata.processing_schedules ps on
                                    pp.processingscheduleid = ps.id
                                left join referencedata.requisition_group_program_schedules rgps on
                                    pp.processingscheduleid = rgps.processingscheduleid
                                left join referencedata.requisition_group_members rgm on
                                    rgm.requisitiongroupid = rgps.requisitiongroupid
                                left join stockmanagement.stock_cards sc on
                                        sc.facilityid = rgm.facilityid
                                    and sc.programid = rgps.programid
                                left join stockmanagement.calculated_stocks_on_hand csoh on
                                    csoh.stockcardid = sc.id
                        where
                            sc.lotid is not null and ps.code in('M1','Q1')) main;

drop table if exists smm;
select * into smm from (select
                            pp.id as periodid,
                            pp.startdate ,
                            pp.enddate ,
                            sc.facilityid ,
                            sc.orderableid ,
                            sc.lotid
                        from
                            referencedata.processing_periods pp
                                left join referencedata.processing_schedules ps on
                                    pp.processingscheduleid = ps.id
                                left join referencedata.requisition_group_program_schedules rgps on
                                    pp.processingscheduleid = rgps.processingscheduleid
                                left join referencedata.requisition_group_members rgm on
                                    rgm.requisitiongroupid = rgps.requisitiongroupid
                                left join stockmanagement.stock_cards sc on
                                        sc.facilityid = rgm.facilityid
                                    and sc.programid = rgps.programid
                        where
                            sc.lotid is not null and ps.code in('M1','Q1')) main;


create view dashboard.vw_historical_data as select
                                                gz_prov.name as province,
                                                gz.name as district,
                                                closesoh.facilityid as facilityinstockcard,
                                                f.id as facilityid,
                                                f.code as facilitycode,
                                                f.name as facilityname,
                                                closesoh.periodid,
                                                closesoh.startdate,
                                                closesoh.enddate,
                                                CONCAT(TO_CHAR(closesoh.startdate, 'DD'), ' ',
                                                       (case TO_CHAR(closesoh.startdate, 'MM')
                                                            when '01' then 'Jan'
                                                            when '02' then 'Fev'
                                                            when '03' then 'Mar'
                                                            when '04' then 'Abr'
                                                            when '05' then 'Mai'
                                                            when '06' then 'Jun'
                                                            when '07' then 'Jul'
                                                            when '08' then 'Ago'
                                                            when '09' then 'Set'
                                                            when '10' then 'Out'
                                                            when '11' then 'Nov'
                                                            when '12' then 'Dez' end), ' ',
                                                       TO_CHAR(closesoh.startdate, 'YYYY'), ' - ', TO_CHAR(closesoh.enddate, 'DD'), ' ',
                                                       (case TO_CHAR(closesoh.enddate, 'MM')
                                                            when '01' then 'Jan'
                                                            when '02' then 'Fev'
                                                            when '03' then 'Mar'
                                                            when '04' then 'Abr'
                                                            when '05' then 'Mai'
                                                            when '06' then 'Jun'
                                                            when '07' then 'Jul'
                                                            when '08' then 'Ago'
                                                            when '09' then 'Set'
                                                            when '10' then 'Out'
                                                            when '11' then 'Nov'
                                                            when '12' then 'Dez' end), ' ', TO_CHAR(closesoh.enddate, 'YYYY')) requisitiononperiod,
                                                o.id as orderableid,
                                                o.code as productcode,
                                                o.fullproductname as productname,
                                                initialsoh.initialstock,
                                                case
                                                    when mov.totalconsumptions is null then 0
                                                    else mov.totalconsumptions
                                                    end as consumptions,
                                                case
                                                    when mov.totalentries is null then 0
                                                    else mov.totalentries
                                                    end as entries,
                                                case
                                                    when mov.totaladjustments is null then 0
                                                    else mov.totaladjustments
                                                    end as adjustments,
                                                closesoh.closestock,
                                                case
                                                    when expired.expiredquantity is null then 0
                                                    else expired.expiredquantity
                                                    end as expiredquantity,
                                                hc.cmm as realcmm,
                                                case
                                                    when hc.cmm is null
                                                        or hc.cmm <= 0::double precision then 0::numeric
		else round(hc.cmm::numeric, 2)
end as cmm,
	case
		when hc.cmm is null
		or hc.cmm <= 0::double precision then 0::numeric
		else round(closesoh.closestock::numeric / round(hc.cmm::numeric, 2), 1)
end as mos,
	case
		when reqandapr.approvedquantity is null then 0
		else reqandapr.approvedquantity
end,
	case
		when reqandapr.requestedquantity is null then 0
		else reqandapr.requestedquantity
end,
	vfs.districtfacilitycode,
    vfs.provincefacilitycode
from
	(
	select
		distinct on
		(a.periodid,
		a.facilityid,
		a.orderableid)
		a.periodid,
		a.startdate,
		a.enddate,
		a.facilityid,
		a.orderableid,
		sum(a.stockonhand) over(partition by a.periodid,
		a.facilityid,
		a.orderableid) as closestock
	from
		(select *,row_number() over (partition by periodid,facilityid,orderableid,lotid order by occurreddate desc) as closestockindex from hdm where occurreddate<enddate) a
	where
		a.closestockindex = 1 ) closesoh
left join (
	select
		distinct on
		(a.periodid,
		a.facilityid,
		a.orderableid)
		a.periodid,
		a.startdate,
		a.enddate,
		a.facilityid,
		a.orderableid,
		sum(a.stockonhand) over(partition by a.periodid,
		a.facilityid,
		a.orderableid) as initialstock
	from
		(select *,row_number() over (partition by periodid,facilityid,orderableid,lotid order by occurreddate desc) as initialstockindex from hdm where occurreddate<startdate) a
	where
		initialstockindex = 1
) initialsoh
on
	closesoh.periodid = initialsoh.periodid
	and closesoh.facilityid = initialsoh.facilityid
	and closesoh.orderableid = initialsoh.orderableid
left join (
	select
		distinct on
		(main.periodid,
		main.facilityid,
		sc.orderableid)
	main.periodid,
		main.facilityid,
		sc.orderableid,
		sum(sc.entries) over (partition by main.periodid,
		main.facilityid,
		sc.orderableid) as totalentries,
		sum(sc.consumption) over (partition by main.periodid,
		main.facilityid,
		sc.orderableid) as totalconsumptions,
		sum(sc.adjustment) over (partition by main.periodid,
		main.facilityid,
		sc.orderableid) as totaladjustments
	from
		(select periodid,startdate,enddate,facilityid,orderableid from smm where enddate<now()) main
	left join (
		select
			sc.id,
			sc.lotid,
			sc.facilityid,
			sc.orderableid,
			scli.occurreddate,
			case
				when scli.sourceid is not null then scli.quantity
			end as entries,
			case
				when scli.destinationid is not null then scli.quantity
			end as consumption,
			case
				when scli.sourceid is null
					and scli.destinationid is null
					and scli.reasonid is not null
					and sclir.reasontype = 'DEBIT' then -scli.quantity
					when scli.sourceid is null
					and scli.destinationid is null
					and scli.reasonid is not null
					and sclir.reasontype <> 'DEBIT' then scli.quantity
				end as adjustment
			from
				stockmanagement.stock_cards sc
			left join (
				select
					*
				from
					stockmanagement.stock_card_line_items
				where
					destinationid is not null
					or reasonid is not null
					or sourceid is not null) scli on
				scli.stockcardid = sc.id
			left join stockmanagement.stock_card_line_item_reasons sclir on
				scli.reasonid = sclir.id
) sc
on
		sc.facilityid = main.facilityid
		and sc.orderableid = main.orderableid
		and sc.occurreddate <= main.enddate
		and sc.occurreddate >= main.startdate
	where
		sc.id is not null) mov
on
	mov.periodid = closesoh.periodid
	and mov.facilityid = closesoh.facilityid
	and mov.orderableid = closesoh.orderableid
left join (
	select
		distinct on
		(main.periodid,
		main.facilityid,
		sc.orderableid)
		main.periodid,
		main.startdate,
		main.enddate,
		main.facilityid,
		sc.orderableid,
		sum(sc.stockonhand) over (partition by main.periodid,
		main.facilityid,
		sc.orderableid) as expiredquantity
	from
		(select * from smm where enddate<now()) main
	left join
(
		select
			sc.facilityid,
			l.expirationdate,
			sc.id,
			sc.orderableid,
			csoh.occurreddate,
			csoh.stockonhand
		from
			stockmanagement.stock_cards sc
		left join referencedata.lots l on
			l.id = sc.lotid
		left join stockmanagement.calculated_stocks_on_hand csoh on
			sc.id = csoh.stockcardid
		where
			l.id is not null) sc
on
		sc.facilityid = main.facilityid
		and sc.expirationdate > main.startdate
		and sc.expirationdate < main.enddate
		and sc.occurreddate <= main.enddate
	where
		sc.id is not null) expired
on
	expired.periodid = closesoh.periodid
	and expired.facilityid = closesoh.facilityid
	and expired.orderableid = closesoh.orderableid
left join (select
	main.*,
	rli.orderableid ,
	rli.approvedquantity ,
	rli.requestedquantity
from
	(select distinct on (periodid,facilityid,orderableid) periodid,startdate,enddate,facilityid from smm where enddate<now()) main
left join requisition.requisitions r on r.processingperiodid = main.periodid
left join requisition.requisition_line_items rli on rli.requisitionid = r.id
left join referencedata.programs p on p.id = r.programid
where p.code='VC' and rli.id is not null) reqandapr
on reqandapr.periodid = closesoh.periodid
	and reqandapr.facilityid = closesoh.facilityid
	and reqandapr.orderableid = closesoh.orderableid
left join referencedata.facilities f on
	f.id = closesoh.facilityid
left join referencedata.geographic_zones gz on
	gz.id = f.geographiczoneid
left join referencedata.geographic_zones gz_prov on
	gz_prov.id = gz.parentid
left join dashboard.vw_facility_supplier vfs on
	vfs.facilitycode = f.code
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
	o.id = closesoh.orderableid
left join
	siglusintegration.hf_cmms hc on
	hc.facilitycode = f.code
	and hc.productcode = o.code
	and hc.periodbegin <= closesoh.enddate
	and hc.periodend >= closesoh.enddate
;