-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
-- Table Definition ----------------------------------------------
drop view if exists dashboard.vw_historical_data;
CREATE TABLE dashboard.historical_data_persistent (
                                                      periodid uuid,
                                                      orderableid uuid,
                                                      initialstock bigint,
                                                      consumptions bigint,
                                                      entries bigint,
                                                      adjustments bigint,
                                                      closestock bigint,
                                                      expiredquantity bigint,
                                                      realcmm double precision,
                                                      cmm numeric,
                                                      mos numeric,
                                                      approvedquantity bigint,
                                                      requestedquantity bigint,
                                                      facilityid uuid
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX historical_data_persistent_periodid_orderableid_facilityid_idx ON dashboard.historical_data_persistent(periodid uuid_ops,orderableid uuid_ops,facilityid uuid_ops);

insert into dashboard.historical_data_persistent (periodid,
                                                  orderableid,
                                                  initialstock,
                                                  consumptions,
                                                  entries,
                                                  adjustments,
                                                  closestock,
                                                  expiredquantity,
                                                  realcmm ,
                                                  cmm ,
                                                  mos ,
                                                  approvedquantity,
                                                  requestedquantity,
                                                  facilityid)
select
    closesoh.periodid,
    o.id                as                                                   orderableid,
    initialsoh.initialstock,
    case
        when mov.totalconsumptions is null then 0
        else mov.totalconsumptions
        end             as                                                   consumptions,
    case
        when mov.totalentries is null then 0
        else mov.totalentries
        end             as                                                   entries,
    case
        when mov.totaladjustments is null then 0
        else mov.totaladjustments
        end             as                                                   adjustments,
    closesoh.closestock,
    case
        when expired.expiredquantity is null then 0
        else expired.expiredquantity
        end             as                                                   expiredquantity,
    hc.cmm              as                                                   realcmm,
    case
        when hc.cmm is null
            or hc.cmm <= 0::double precision then 0::numeric
			else round(hc.cmm::numeric, 2)
end
as cmm,
			case
				when hc.cmm is null
				or hc.cmm <= 0::double precision then 0::numeric
				else round(closesoh.closestock::numeric / round(hc.cmm::numeric, 2), 1)
end
as mos,
			case
				when reqandapr.approvedquantity is null then 0
				else reqandapr.approvedquantity
end
,
			case
				when reqandapr.requestedquantity is null then 0
				else reqandapr.requestedquantity
end,
       f.id                as                                                   facilityid
from
	(
select
	sohbylot.periodid,
	sohbylot.facilityid,
	sohbylot.orderableid,
	sum(sohbylot.stockonhand) as closestock
from
	(
	select
		main.*,
		sc.orderableid,
		sc.lotid,
		csoh.occurreddate,
		csoh.stockonhand,
		row_number() over(partition by main.periodid,
		sc.facilityid,
		sc.lotid order by csoh.occurreddate desc)
	from
		(select
			pp.id as periodid,
			sc.programid,
			sc.facilityid
		from
			referencedata.processing_periods pp
		left join referencedata.processing_schedules ps on
			pp.processingscheduleid = ps.id
		join referencedata.requisition_group_program_schedules rgps on
			pp.processingscheduleid = rgps.processingscheduleid
		join referencedata.requisition_group_members rgm on
			rgm.requisitiongroupid = rgps.requisitiongroupid
		join stockmanagement.stock_cards sc on sc.facilityid=rgm.facilityid and sc.programid=rgps.programid
		where
			ps.code in('M1', 'Q1') and sc.lotid is not null and pp.enddate<now()
		group by sc.programid,sc.facilityid,pp.id) main
	left join referencedata.processing_periods pp on pp.id = main.periodid
	join stockmanagement.stock_cards sc on
		sc.facilityid = main.facilityid
		and sc.programid = main.programid
	join stockmanagement.calculated_stocks_on_hand csoh on
		csoh.stockcardid = sc.id
		and csoh.occurreddate<pp.startdate
	where
		sc.lotid is not null) sohbylot
where sohbylot.row_number=1
group by sohbylot.periodid, sohbylot.facilityid, sohbylot.orderableid) closesoh
left join referencedata.processing_periods pp on closesoh.periodid = pp.id
left join (
	select
	sohbylot.periodid,
	sohbylot.facilityid,
	sohbylot.orderableid,
	sum(sohbylot.stockonhand) as initialstock
from
	(
	select
		main.*,
		sc.orderableid,
		sc.lotid,
		csoh.occurreddate,
		csoh.stockonhand,
		row_number() over(partition by main.periodid,
		sc.facilityid,
		sc.lotid order by csoh.occurreddate desc)
	from
		(select
			pp.id as periodid,
			sc.programid,
			sc.facilityid
		from
			referencedata.processing_periods pp
		left join referencedata.processing_schedules ps on
			pp.processingscheduleid = ps.id
		join referencedata.requisition_group_program_schedules rgps on
			pp.processingscheduleid = rgps.processingscheduleid
		join referencedata.requisition_group_members rgm on
			rgm.requisitiongroupid = rgps.requisitiongroupid
		join stockmanagement.stock_cards sc on sc.facilityid=rgm.facilityid and sc.programid=rgps.programid
		where
			ps.code in('M1', 'Q1') and sc.lotid is not null and pp.enddate<now()
		group by sc.programid,sc.facilityid,pp.id) main
	left join referencedata.processing_periods pp on pp.id = main.periodid
	join stockmanagement.stock_cards sc on
		sc.facilityid = main.facilityid
		and sc.programid = main.programid
	join stockmanagement.calculated_stocks_on_hand csoh on
		csoh.stockcardid = sc.id
		and csoh.occurreddate<pp.startdate
	where
		sc.lotid is not null) sohbylot
where sohbylot.row_number=1
group by sohbylot.periodid, sohbylot.facilityid, sohbylot.orderableid) initialsoh
on
	closesoh.periodid = initialsoh.periodid
	and closesoh.facilityid = initialsoh.facilityid
	and closesoh.orderableid = initialsoh.orderableid
left join (	 SELECT DISTINCT ON (main.periodid, main.facilityid, sc.orderableid) main.periodid,
    main.facilityid,
    sc.orderableid,
    sum(sc.entries) OVER (PARTITION BY main.periodid, main.facilityid, sc.orderableid) AS totalentries,
    sum(sc.consumption) OVER (PARTITION BY main.periodid, main.facilityid, sc.orderableid) AS totalconsumptions,
    sum(sc.adjustment) OVER (PARTITION BY main.periodid, main.facilityid, sc.orderableid) AS totaladjustments
   FROM ( SELECT pp_1.id AS periodid,
            sc_1.programid,
            sc_1.facilityid
           FROM referencedata.processing_periods pp_1
             LEFT JOIN referencedata.processing_schedules ps ON pp_1.processingscheduleid = ps.id
             JOIN referencedata.requisition_group_program_schedules rgps ON pp_1.processingscheduleid = rgps.processingscheduleid
             JOIN referencedata.requisition_group_members rgm ON rgm.requisitiongroupid = rgps.requisitiongroupid
             JOIN stockmanagement.stock_cards sc_1 ON sc_1.facilityid = rgm.facilityid AND sc_1.programid = rgps.programid
          WHERE (ps.code = ANY (ARRAY['M1'::text, 'Q1'::text])) AND sc_1.lotid IS NOT NULL AND pp_1.enddate < now()
          GROUP BY sc_1.programid, sc_1.facilityid, pp_1.id) main
     LEFT JOIN referencedata.processing_periods pp ON main.periodid = pp.id
     LEFT JOIN ( SELECT sc_1.id,
            sc_1.lotid,
            sc_1.facilityid,
            sc_1.orderableid,
            sc_1.programid,
            scli.occurreddate,
                CASE
                    WHEN scli.sourceid IS NOT NULL THEN scli.quantity
                    ELSE NULL::integer
                END AS entries,
                CASE
                    WHEN scli.destinationid IS NOT NULL THEN scli.quantity
                    ELSE NULL::integer
                END AS consumption,
                CASE
                    WHEN scli.sourceid IS NULL AND scli.destinationid IS NULL AND scli.reasonid IS NOT NULL AND sclir.reasontype = 'DEBIT'::text THEN - scli.quantity
                    WHEN scli.sourceid IS NULL AND scli.destinationid IS NULL AND scli.reasonid IS NOT NULL AND sclir.reasontype <> 'DEBIT'::text THEN scli.quantity
                    ELSE NULL::integer
                END AS adjustment
           FROM stockmanagement.stock_cards sc_1
             LEFT JOIN ( SELECT * FROM stockmanagement.stock_card_line_items
                  WHERE stock_card_line_items.destinationid IS NOT NULL OR stock_card_line_items.reasonid IS NOT NULL OR stock_card_line_items.sourceid IS NOT NULL) scli ON scli.stockcardid = sc_1.id
             LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON scli.reasonid = sclir.id) sc ON sc.facilityid = main.facilityid AND sc.programid = main.programid AND sc.occurreddate <= pp.enddate AND sc.occurreddate >= pp.startdate
  WHERE sc.id IS NOT NULL) mov
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
		pp.startdate,
		pp.enddate,
		main.facilityid,
		sc.orderableid,
		sum(sc.stockonhand) over (partition by main.periodid,
		main.facilityid,
		sc.orderableid) as expiredquantity
	from
		(select
			pp.id as periodid,
			sc.programid,
			sc.facilityid
		from
			referencedata.processing_periods pp
		left join referencedata.processing_schedules ps on
			pp.processingscheduleid = ps.id
		join referencedata.requisition_group_program_schedules rgps on
			pp.processingscheduleid = rgps.processingscheduleid
		join referencedata.requisition_group_members rgm on
			rgm.requisitiongroupid = rgps.requisitiongroupid
		join stockmanagement.stock_cards sc on sc.facilityid=rgm.facilityid and sc.programid=rgps.programid
		where
			ps.code in('M1', 'Q1') and sc.lotid is not null and pp.enddate<now()
		group by sc.programid,sc.facilityid,pp.id) main
	left join referencedata.processing_periods pp on pp.id = main.periodid
	left join
(
		select
			sc.facilityid,
			l.expirationdate,
			sc.id,
			sc.orderableid,
			csoh.occurreddate,
			csoh.stockonhand,
			sc.programid
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
		and sc.programid = main.programid
		and sc.expirationdate > pp.startdate
		and sc.expirationdate < pp.enddate
		and sc.occurreddate <= pp.enddate
	where
		sc.id is not null) expired
on
	expired.periodid = closesoh.periodid
	and expired.facilityid = closesoh.facilityid
	and expired.orderableid = closesoh.orderableid
left join (select
	main.periodid,
	main.facilityid,
	main.programid,
	main.orderableid,
	rrli.approvedquantity,
	rrli.requestedquantity
from
	(
	select
		pp.id as periodid,
		sc.programid,
		sc.orderableid,
		sc.facilityid
	from
		referencedata.processing_periods pp
	left join referencedata.processing_schedules ps on
		pp.processingscheduleid = ps.id
	join referencedata.requisition_group_program_schedules rgps on
		pp.processingscheduleid = rgps.processingscheduleid
	join referencedata.requisition_group_members rgm on
		rgm.requisitiongroupid = rgps.requisitiongroupid
	join stockmanagement.stock_cards sc on
		sc.facilityid = rgm.facilityid
		and sc.programid = rgps.programid
	where
		ps.code in('M1', 'Q1')
		and sc.lotid is not null
		and pp.enddate<now()
	group by
		sc.orderableid,
		sc.facilityid,
		sc.programid,
		pp.id ) main
left join (
	select
		r.*,
		rli.orderableid,
		rli.approvedquantity ,
		rli.requestedquantity
	from
		requisition.requisitions r
	left join requisition.requisition_line_items rli on
		r.id = rli.requisitionid) rrli on
	main.periodid = rrli.processingperiodid
	and main.orderableid = rrli.orderableid
	and main.facilityid = rrli.facilityid
left join referencedata.programs p on
	p.id = main.programid
where
	p.code = 'VC'
	and rrli.id is not null) reqandapr
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
	and hc.periodbegin <= pp.enddate
	and hc.periodend >= pp.enddate
;