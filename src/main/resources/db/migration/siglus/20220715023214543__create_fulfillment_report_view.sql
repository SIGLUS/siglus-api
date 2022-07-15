-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop view if exists dashboard.vw_fulfillment_report;
create view dashboard.vw_fulfillment_report
            (
             approvedquantity,
             district,
             districtfacilitycode,
             facilitymergetype,
             facilitytype,
             ficilityname,
             ficilitycode,
             fulfillrate,
             fulfillratenum,
             maxshippeddate,
             productcode,
             originalperiod,
             productname,
             program,
             province,
             provincefacilitycode,
             reporttype,
             requestedquantity,
             requisitionperiod,
             sumquantityshipped
                )
as
(

with temp_all as
         (select fz.Province         province,
                 fz.District         district,
                 fz.name             ficilityname,
                 rf.code             ficilitycode,
                 pp.enddate          requisitionperiod,
                 CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ', (case TO_CHAR(pp.startdate, 'MM')
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
                             when '12' then 'Dez' end), ' ', TO_CHAR(pp.enddate, 'YYYY'))
                                     originalperiod,
                 (CASE
                      WHEN r.emergency IS TRUE THEN 'Emergency'
                      ELSE 'Regular'
                     END)            reporttype,
                 t.maxshippeddate,
                 o.fullproductname   productname,
                 rli.requestedquantity,
                 rli.approvedquantity,
                 t.sumquantityshipped,
                 p.name as           program,
                 (case
                      when approvedquantity > 0 is true
                          then concat(round(sumquantityshipped * 100.0 / approvedquantity, 2), '%')
                      else '0%' end) fulfillrate,
                 (case
                      when approvedquantity > 0 is true
                          then round(sumquantityshipped * 100.0 / approvedquantity, 2)
                      else 0 end)    fulfillratenum,
                 ft.code             facilitytype,
                 ftm.category        facilitymergetype,
                 vfs.districtfacilitycode,
                 vfs.provincefacilitycode,
                 o.code              productcode
          from requisition.requisition_line_items rli
                   LEFT JOIN requisition.requisitions r ON r.id = rli.requisitionid
                   LEFT JOIN
               (SELECT f.id, f.name, zz.District District, zz.Province Province
                FROM referencedata.facilities f
                         LEFT JOIN
                     (SELECT z1.id id, z1.name District, z2.name Province
                      FROM referencedata.geographic_zones z1
                               LEFT JOIN referencedata.geographic_zones z2 ON z1.parentid = z2.id) zz
                     ON f.geographiczoneid = zz.id) fz ON r.facilityid = fz.id
                   LEFT JOIN referencedata.facilities rf ON r.facilityid = rf.id
                   LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = rf.code
                   LEFT JOIN referencedata.programs p ON r.programid = p.id
                   LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id
                   LEFT JOIN referencedata.orderables o ON rli.orderableid = o.id
                   left join referencedata.facility_types ft on rf.typeid = ft.id
                   left join siglusintegration.facility_type_mapping ftm on ftm.facilitytypecode = ft.code
                   left join (select sli.orderableid          orderableid,
                                     (CASE
                                          WHEN oei.requisitionid is null IS TRUE THEN orders.externalid
                                          ELSE oei.requisitionid
                                         END)                 realrequisitionid,
                                     max(s.shippeddate)       maxshippeddate,
                                     sum(sli.quantityshipped) sumquantityshipped
                              from fulfillment.shipment_line_items sli
                                       left join fulfillment.shipments s on sli.shipmentid = s.id
                                       left join fulfillment.orders orders on s.orderid = orders.id
                                       left join siglusintegration.order_external_ids oei on orders.externalid = oei.id
                              group by orderableid, realrequisitionid) t
                             on rli.requisitionid = t.realrequisitionid and rli.orderableid = t.orderableid
          where r.status in ('APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER'))
    (select t.approvedquantity,
            t.district,
            t.districtfacilitycode,
            t.facilitymergetype,
            t.facilitytype,
            t.ficilityname,
            t.ficilitycode,
            t.fulfillrate,
            t.fulfillratenum,
            t.maxshippeddate,
            t.productcode,
            t.originalperiod,
            t.productname,
            t.program,
            t.province,
            t.provincefacilitycode,
            t.reporttype,
            t.requestedquantity,
            t.requisitionperiod,
            t.sumquantityshipped
     from temp_all t)
union all
(select t.approvedquantity,
        t.district,
        t.districtfacilitycode,
        t.facilitymergetype,
        t.facilitytype,
        t.ficilityname,
        t.ficilitycode,
        t.fulfillrate,
        t.fulfillratenum,
        t.maxshippeddate,
        t.productcode,
        t.originalperiod,
        t.productname,
        'Malaria' as program,
        t.province,
        t.provincefacilitycode,
        t.reporttype,
        t.requestedquantity,
        t.requisitionperiod,
        t.sumquantityshipped
 from temp_all t
 where productcode in (select distinct o.code
                       from referencedata.orderables o
                       where o.id in
                             (select additionalorderableid
                              from siglusintegration.program_additional_orderables ao
                                       left join referencedata.programs p on p.id = ao.programid
                              where p.code = 'ML')))
);
