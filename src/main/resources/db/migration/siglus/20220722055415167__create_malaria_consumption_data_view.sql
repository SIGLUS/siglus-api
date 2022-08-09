-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_malaria_consumption_data;
create view dashboard.vw_malaria_consumption_data as select
                                                         gz_prov.name as province,
                                                         gz.name as district,
                                                         ft.code as facilitytype,
                                                         f.code as facilitycode,
                                                         f.name as facilityname,
                                                         main.*,
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
                                                                     when '12' then 'Dez' end), ' ', TO_CHAR(pp.enddate, 'YYYY')) period,
                                                         pp.enddate,
                                                         vfs.provincefacilitycode,
                                                         vfs.districtfacilitycode
                                                     from
                                                         (
                                                             select
                                                                 a.requisitionid,
                                                                 a.type,
                                                                 sum("6x1 treatement") as "treatement6x1",
                                                                 sum("6x2 treatement") as "treatement6x2",
                                                                 sum("6x3 treatement") as "treatement6x3",
                                                                 sum("6x4 treatement") as "treatement6x4",
                                                                 sum("6x1 existentStock") as "stock6x1",
                                                                 sum("6x2 existentStock") as "stock6x2",
                                                                 sum("6x3 existentStock") as "stock6x3",
                                                                 sum("6x4 existentStock") as "stock6x4"
                                                             from
                                                                 (
                                                                     select
                                                                         uili.id,
                                                                         uili.requisitionid,
                                                                         uili.information,
                                                                         uili.service,
                                                                         o.code,
                                                                         o.fullproductname ,
                                                                         case
                                                                             uili.service
                                                                             when 'HF' then 'HF'
                                                                             when 'newColumn0' then 'CHW'
                                                                             end as type,
                                                                         case
                                                                             when uili.information = 'treatmentsAttended'
                                                                                 and code = '08O05' then value
                                                                             end as "6x1 treatement",
                                                                         case
                                                                             when uili.information = 'treatmentsAttended'
                                                                                 and code = '08O05Z' then value
                                                                             end as "6x2 treatement",
                                                                         case
                                                                             when uili.information = 'treatmentsAttended'
                                                                                 and code = '08O05Y' then value
                                                                             end as "6x3 treatement",
                                                                         case
                                                                             when uili.information = 'treatmentsAttended'
                                                                                 and code = '08O05X' then value
                                                                             end as "6x4 treatement",
                                                                         case
                                                                             when uili.information = 'existentStock'
                                                                                 and code = '08O05' then value
                                                                             end as "6x1 existentStock",
                                                                         case
                                                                             when uili.information = 'existentStock'
                                                                                 and code = '08O05Z' then value
                                                                             end as "6x2 existentStock",
                                                                         case
                                                                             when uili.information = 'existentStock'
                                                                                 and code = '08O05Y' then value
                                                                             end as "6x3 existentStock",
                                                                         case
                                                                             when uili.information = 'existentStock'
                                                                                 and code = '08O05X' then value
                                                                             end as "6x4 existentStock"
                                                                     from
                                                                         siglusintegration.usage_information_line_items uili
                                                                             left join (
                                                                             select
                                                                                 id,
                                                                                 code,
                                                                                 fullproductname
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
                                                                                               uili.orderableid = o.id
                                                                     where
                                                                             uili.service <> 'total'
                                                                       and o.code in ('08O05', '08O05Z', '08O05Y', '08O05X')
                                                                       and uili.value is not null) a
                                                             group by
                                                                 a.requisitionid ,
                                                                 a.type) main
                                                             left join requisition.requisitions r on
                                                                 r.id = main.requisitionid
                                                             left join referencedata.processing_periods pp on
                                                                 pp.id = r.processingperiodid
                                                             left join referencedata.facilities f on
                                                                 f.id = r.facilityid
                                                             LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
                                                             LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
                                                             LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
                                                             LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
                                                     where f.id is not null;