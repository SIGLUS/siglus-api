-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


drop view if exists dashboard.vw_fulfillment_report;
create view dashboard.vw_fulfillment_report
            (
             rliid,
             approvedquantity,
             district,
             districtfacilitycode,
             facilitymergetype,
             facilityid,
             facilitytype,
             facilityname,
             facilitycode,
             fulfillrate,
             fulfillrequestedrate,
             maxshippeddate,
             productcode,
             originalperiod,
             productname,
             program,
             programcode,
             province,
             provincefacilitycode,
             reporttype,
             requestedquantity,
             requisitionperiod,
             periodid,
             sumquantityshipped
                )
as
(
with
    temp_malaria_product_code as (select distinct o.code
                                  from referencedata.orderables o
                                  where o.id in
                                        (select additionalorderableid
                                         from siglusintegration.program_additional_orderables ao
                                                  left join referencedata.programs p on p.id = ao.programid
                                         where p.code = 'ML')),
    temp_all_line_items as (select rli.id              rliid,
                                   fz.Province         province,
                                   fz.District         district,
                                   r.facilityid        facilityid,
                                   fz.name             ficilityname,
                                   rf.code             ficilitycode,
                                   pp.enddate          requisitionperiod,
                                   pp.id               periodid,
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
                                   cast(o.fullproductname as text)   productname,
                                   rli.requestedquantity,
                                   rli.approvedquantity,
                                   t.sumquantityshipped,
                                   p.name as           program,
                                   p.code as           programcode,
                                   (case
                                        when approvedquantity > 0 is true
                                            then sumquantityshipped * 1.0 / approvedquantity
                                        else 0 end) fulfillrate,
                                   (case
                                        when requestedquantity > 0 is true
                                            then sumquantityshipped * 1.0 / requestedquantity
                                        else 0 end) fulfillrequestedrate,
                                   ft.code             facilitytype,
                                   ftm.category        facilitymergetype,
                                   vfs.districtfacilitycode,
                                   vfs.provincefacilitycode,
                                   o.code              productcode
                            from (SELECT sli.orderableid          orderableid,
                                         (CASE
                                              WHEN oei.requisitionid IS NULL IS TRUE THEN orders.externalid
                                              ELSE oei.requisitionid
                                             END)                 realrequisitionid,
                                         MAX(s.shippeddate)       maxshippeddate,
                                         SUM(sli.quantityshipped) sumquantityshipped
                                  FROM fulfillment.shipment_line_items sli
                                           LEFT JOIN fulfillment.shipments s ON sli.shipmentid = s.id
                                           LEFT JOIN fulfillment.orders orders ON s.orderid = orders.id
                                           LEFT JOIN siglusintegration.order_external_ids oei ON orders.externalid = oei.id
                                  GROUP BY orderableid, realrequisitionid) t
                                     LEFT JOIN requisition.requisition_line_items rli
                                               ON rli.requisitionid = t.realrequisitionid AND
                                                  rli.orderableid = t.orderableid
                                     LEFT JOIN requisition.requisitions r ON r.id = t.realrequisitionid
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
                                     LEFT JOIN (SELECT DISTINCT ON
                                (orderables.id) orderables.id,
                                                FIRST_VALUE(orderables.code)
                                                OVER (PARTITION BY orderables.id ORDER BY orderables.versionnumber DESC) AS code,
                                                FIRST_VALUE(orderables.fullproductname)
                                                OVER (PARTITION BY orderables.id ORDER BY orderables.versionnumber DESC) AS fullproductname
                                                FROM referencedata.orderables) o ON t.orderableid = o.id
                                     left join referencedata.facility_types ft on rf.typeid = ft.id
                                     left join siglusintegration.facility_type_mapping ftm on ftm.facilitytypecode = ft.code
                            where r.status in ('APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') and rli.approvedquantity > 0)
-- Regular
    (select t.rliid,
            t.approvedquantity,
            t.district,
            t.districtfacilitycode,
            t.facilitymergetype,
            t.facilityid,
            t.facilitytype,
            t.ficilityname,
            t.ficilitycode,
            t.fulfillrate,
            t.fulfillrequestedrate,
            t.maxshippeddate,
            t.productcode,
            t.originalperiod,
            t.productname,
            (case when t.productcode in (select code from temp_malaria_product_code) is true then 'Malaria' else t.program end) as program,
            (case when t.productcode in (select code from temp_malaria_product_code) is true then 'ML' else t.programcode end) programcode,
            t.province,
            t.provincefacilitycode,
            t.reporttype,
            t.requestedquantity,
            t.requisitionperiod,
            t.periodid,
            t.sumquantityshipped
     from temp_all_line_items t
     where t.programcode != 'VC'
        or (t.programcode = 'VC' and t.reporttype = 'Regular'))
-- --  via & Emergency
union all
(select distinct on (t.ficilitycode, t.periodid, t.productcode) t.rliid,
                                                                sum(t.approvedquantity)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode) as approvedquantity,
                                                                t.district,
                                                                t.districtfacilitycode,
                                                                t.facilitymergetype,
                                                                t.facilityid,
                                                                t.facilitytype,
                                                                t.ficilityname,
                                                                t.ficilitycode,
                                                                (case
                                                                     when sum(t.approvedquantity)
                                                                          over (partition by t.ficilitycode, t.periodid, t.productcode) >
                                                                          0 is true
                                                                         then sum(t.sumquantityshipped)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode) *
                                                                              1.0 /
                                                                              sum(t.approvedquantity)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode)
                                                                     else 0 end)                                              fulfillrate,
                                                                (case
                                                                     when sum(t.requestedquantity)
                                                                          over (partition by t.ficilitycode, t.periodid, t.productcode) >
                                                                          0 is true
                                                                         then sum(t.sumquantityshipped)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode) *
                                                                              1.0 /
                                                                              sum(t.requestedquantity)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode)
                                                                     else 0 end)                                              fulfillrequestedrate,
                                                                max(t.maxshippeddate)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    maxshippeddate,
                                                                t.productcode,
                                                                t.originalperiod,
                                                                t.productname,
                                                                t.program,
                                                                t.programcode,
                                                                t.province,
                                                                t.provincefacilitycode,
                                                                t.reporttype,
                                                                sum(t.requestedquantity)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    requestedquantity,
                                                                t.requisitionperiod,
                                                                t.periodid,
                                                                sum(t.sumquantityshipped)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    sumquantityshipped
 from temp_all_line_items t
 where t.programcode = 'VC'
   and t.reporttype = 'Emergency' and t.productcode not in (select code from temp_malaria_product_code))
--  Malaria & Emergency
union all
(select distinct on (t.ficilitycode, t.periodid, t.productcode) t.rliid,
                                                                sum(t.approvedquantity)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode) as approvedquantity,
                                                                t.district,
                                                                t.districtfacilitycode,
                                                                t.facilitymergetype,
                                                                t.facilityid,
                                                                t.facilitytype,
                                                                t.ficilityname,
                                                                t.ficilitycode,
                                                                (case
                                                                     when sum(t.approvedquantity)
                                                                          over (partition by t.ficilitycode, t.periodid, t.productcode) >
                                                                          0 is true
                                                                         then sum(t.sumquantityshipped)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode) *
                                                                              1.0 /
                                                                              sum(t.approvedquantity)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode)
                                                                     else 0 end)                                              fulfillrate,
                                                                (case
                                                                     when sum(t.requestedquantity)
                                                                          over (partition by t.ficilitycode, t.periodid, t.productcode) >
                                                                          0 is true
                                                                         then sum(t.sumquantityshipped)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode) *
                                                                              1.0 /
                                                                              sum(t.requestedquantity)
                                                                              over (partition by t.ficilitycode, t.periodid, t.productcode)
                                                                     else 0 end)                                              fulfillrequestedrate,
                                                                max(t.maxshippeddate)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    maxshippeddate,
                                                                t.productcode,
                                                                t.originalperiod,
                                                                t.productname,
                                                                'Malaria' as program,
                                                                'ML'      as programcode,
                                                                t.province,
                                                                t.provincefacilitycode,
                                                                t.reporttype,
                                                                sum(t.requestedquantity)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    requestedquantity,
                                                                t.requisitionperiod,
                                                                t.periodid,
                                                                sum(t.sumquantityshipped)
                                                                over (partition by t.ficilitycode, t.periodid, t.productcode)    sumquantityshipped
 from temp_all_line_items t
 where t.programcode = 'VC'
   and t.reporttype = 'Emergency' and t.productcode in (select code from temp_malaria_product_code))
);
