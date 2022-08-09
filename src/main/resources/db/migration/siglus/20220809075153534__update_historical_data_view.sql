-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop materialized view if exists dashboard.vw_historical_data;
create materialized view dashboard.vw_historical_data as
select
    pp.id as periodid,
    pp.startdate ,
    pp.enddate ,
    o.id as orderableid,
    f.id as facilityid,
    gz_prov.name as province,
    gz.name as district,
    ft.code as facilitytype,
    f.code as facilitycode,
    f.name as facilityname,
    o.code as productcode,
    o.fullproductname as productname,
    case
        when rli.beginningbalance is null then 'N/A'
        else rli.beginningbalance::text
        end as initialstock,
    rli.adjustedconsumption as consumptions,
    rli.totalreceivedquantity as entries,
    rli.totallossesandadjustments as adjustments,
    rli.stockonhand as closestock,
    CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ',
           (case TO_CHAR(pp.startdate, 'MM')
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
           TO_CHAR(pp.startdate, 'YYYY'), ' - ', TO_CHAR(pp.enddate, 'DD'), ' ',
           (case TO_CHAR(pp.enddate, 'MM')
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
                when '12' then 'Dez' end), ' ', TO_CHAR(pp.enddate, 'YYYY')) requisitiononperiod,
    case
        when hc.cmm is null
            or hc.cmm <= cast(0 as double precision) then cast(0 as numeric)
        else round(cast(hc.cmm as numeric), 2)
        end as cmm,
    case
        when hc.cmm is null
            or hc.cmm <= cast(0 as double precision) then cast(0 as numeric)
        else round(cast(rli.stockonhand as numeric) / round(cast(hc.cmm as numeric), 2), 1)
        end as mos,
    case
        when appandreq.approvedquantity is null
            or p.code <> 'VC' then 0
        else appandreq.approvedquantity
        end as approvedquantity,
    case
        when appandreq.requestedquantity is null
            or p.code <> 'VC' then 0
        else appandreq.requestedquantity
        end as requestedquantity,
    expired.expiredquantity,
    vfs.provincefacilitycode,
    vfs.districtfacilitycode,
    case
        when ps.code = 'M1' then 'monthly'
        when ps.code = 'Q1' then 'quarterly'
        end as periodtype
from
    requisition.requisition_line_items rli
        left join requisition.requisitions r on
            rli.requisitionid = r.id
        left join referencedata.programs p on
            p.id = r.programid
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
                o.versionnumber = o.latestversion) o
                  on
                          rli.orderableid = o.id
        left join referencedata.facilities f on
            f.id = r.facilityid
        left join referencedata.facility_types ft on
            f.typeid = ft.id
        left join referencedata.geographic_zones gz on
            gz.id = f.geographiczoneid
        left join referencedata.geographic_zones gz_prov on
            gz_prov.id = gz.parentid
        left join dashboard.vw_facility_supplier vfs on
            vfs.facilitycode = f.code
        left join referencedata.processing_periods pp on
            pp.id = r.processingperiodid
        left join
    siglusintegration.hf_cmms hc on
                hc.facilitycode = f.code
            and hc.productcode = o.code
            and hc.periodbegin = pp.startdate
            and hc.periodend = pp.enddate
        left join (
        select
            e.facilityid,e.periodid,e.orderableid,sum(e.expiredquantity) as expiredquantity
        from
            (
                select
                    r.facilityid ,
                    rli.orderableid ,
                    pp.id as periodid,
                    sc.lotid,
                    csoh.stockonhand as expiredquantity,
                    row_number() over(partition by r.facilityid,
		rli.orderableid ,
		pp.id,
		sc.lotid
	order by
		csoh.occurreddate desc)
                from
                    stockmanagement.stock_cards sc
                        join requisition.requisition_line_items rli on
                            rli.orderableid = sc.orderableid
                        join requisition.requisitions r on
                                r.id = rli.requisitionid
                            and r.facilityid = sc.facilityid
                        left join referencedata.processing_periods pp on
                            r.processingperiodid = pp.id
                        left join stockmanagement.calculated_stocks_on_hand csoh on
                                csoh.stockcardid = sc.id
                            and csoh.occurreddate <= pp.enddate
                            and csoh.occurreddate >= pp.startdate
                        left join referencedata.lots l on
                                l.id = sc.lotid
                            and l.expirationdate <= pp.enddate
                            and l.expirationdate >= pp.startdate
                where
                    l.id is not null
                  and r.emergency = false) e
        where
                row_number = 1
        group by e.facilityid,e.periodid,e.orderableid) expired on
                r.facilityid = expired.facilityid
            and rli.orderableid = expired.orderableid
            and r.processingperiodid = expired.periodid
        left join referencedata.processing_schedules ps on
            pp.processingscheduleid = ps.id
        left join (select
                       r.processingperiodid ,
                       r.facilityid ,
                       rli.orderableid ,
                       sum(rli.approvedquantity) as approvedquantity,
                       sum(rli.requestedquantity) as requestedquantity
                   from
                       requisition.requisition_line_items rli
                           join requisition.requisitions r on
                               r.id = rli.requisitionid
                   group by
                       r.processingperiodid ,
                       r.facilityid ,
                       rli.orderableid) appandreq on appandreq.processingperiodid=pp.id and appandreq.facilityid = r.facilityid and appandreq.orderableid=rli.orderableid
where
        ps.code in('M1', 'Q1')
  and r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER')
  and r.emergency = false with no data;