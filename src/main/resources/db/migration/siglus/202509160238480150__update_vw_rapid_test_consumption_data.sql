-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_rapid_test_consumption_data;
create view dashboard.vw_rapid_test_consumption_data as
select gz_prov.name as                                                          province,
       gz.name      as                                                          district,
       ft.code      as                                                          facilitytype,
       f.code       as                                                          facilitycode,
       f.name       as                                                          facilityname,
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
       main.*,
       pp.enddate,
       vfs.provincefacilitycode,
       vfs.districtfacilitycode
from (select a.requisitionid,
             a.service,
             sum(a.consumehivdetermine)     as consumehivdetermine,
             sum(a.positivehivdetermine)    as positivehivdetermine,
             sum(a.unjustifiedhivdetermine) as unjustifiedhivdetermine,
             sum(a.consumehivunigold)       as consumehivunigold,
             sum(a.positivehivunigold)      as positivehivunigold,
             sum(a.unjustifiedhivunigold)   as unjustifiedhivunigold,
             sum(a.consumesyphillis)        as consumesyphillis,
             sum(a.positivesyphillis)       as positivesyphillis,
             sum(a.unjustifiedsyphillis)    as unjustifiedsyphillis,
             sum(a.consumemalaria)          as consumemalaria,
             sum(a.positivemalaria)         as positivemalaria,
             sum(a.unjustifiedmalaria)      as unjustifiedmalaria,
             sum(a.consumeduotestehivesifilis)      as consumeduotestehivesifilis,
             sum(a.positivehivduotestehivesifilis)      as positivehivduotestehivesifilis,
             sum(a.positivesyphilisduotestehivesifilis)      as positivesyphilisduotestehivesifilis,
             sum(a.unjustifiedduotestehivesifilis)      as unjustifiedduotestehivesifilis,
             sum(a.consumehepatitebtestes)      as consumehepatitebtestes,
             sum(a.positivehepatitebtestes)      as positivehepatitebtestes,
             sum(a.unjustifiedhepatitebtestes)      as unjustifiedhepatitebtestes,
             sum(a.consumetdroraldehiv)      as consumetdroraldehiv,
             sum(a.positivetdroraldehiv)      as positivetdroraldehiv,
             sum(a.unjustifiedtdroraldehiv)      as unjustifiedtdroraldehiv,
             sum(a.consumenovoteste)      as consumenovoteste,
             sum(a.positivenovoteste)      as positivenovoteste,
             sum(a.unjustifiednovoteste)      as unjustifiednovoteste
      from (select r.id    as requisitionid,
                   rtcli.project,
                   rtcli.outcome,
                   rtcli.value,
                   case
                       rtcli.service
                       when 'HF' then 'Maternidade_SMI'
                       when 'newColumn0' then 'Enfermaria'
                       when 'newColumn3' then 'Banco de Socorro-BIS'
                       when 'newColumn4' then 'Brigade movel'
                       when 'newColumn5' then 'Laboratorio'
                       when 'newColumn1' then 'UATS'
                       when 'newColumn2' then 'PAV'
                       when 'newColumn6' then 'PNCTL'
                       when 'newColumn7' then 'Estomatologia'
                       when 'APES' then 'APEs'
                       end as service,
                   case
                       when rtcli.project = 'hivDetermine'
                           and rtcli.outcome = 'consumo' then value
                       end as consumehivdetermine,
                   case
                       when rtcli.project = 'hivDetermine'
                           and rtcli.outcome = 'positive' then value
                       end as positivehivdetermine,
                   case
                       when rtcli.project = 'hivDetermine'
                           and rtcli.outcome = 'unjustified' then value
                       end as unjustifiedhivdetermine,
                   case
                       when rtcli.project = 'newColumn0'
                           and rtcli.outcome = 'consumo' then value
                       end as consumehivunigold,
                   case
                       when rtcli.project = 'newColumn0'
                           and rtcli.outcome = 'positive' then value
                       end as positivehivunigold,
                   case
                       when rtcli.project = 'newColumn0'
                           and rtcli.outcome = 'unjustified' then value
                       end as unjustifiedhivunigold,
                   case
                       when rtcli.project = 'newColumn1'
                           and rtcli.outcome = 'consumo' then value
                       end as consumesyphillis,
                   case
                       when rtcli.project = 'newColumn1'
                           and rtcli.outcome = 'positive' then value
                       end as positivesyphillis,
                   case
                       when rtcli.project = 'newColumn1'
                           and rtcli.outcome = 'unjustified' then value
                       end as unjustifiedsyphillis,
                   case
                       when rtcli.project = 'newColumn2'
                           and rtcli.outcome = 'consumo' then value
                       end as consumemalaria,
                   case
                       when rtcli.project = 'newColumn2'
                           and rtcli.outcome = 'positive' then value
                       end as positivemalaria,
                   case
                       when rtcli.project = 'newColumn2'
                           and rtcli.outcome = 'unjustified' then value
                       end as unjustifiedmalaria,
                   case
                       when rtcli.project = 'newColumn3'
                           and rtcli.outcome = 'consumo' then value
                       end as consumeduotestehivesifilis,
                   case
                       when rtcli.project = 'newColumn3'
                           and rtcli.outcome = 'positive_hiv' then value
                       end as positivehivduotestehivesifilis,
                   case
                       when rtcli.project = 'newColumn3'
                           and rtcli.outcome = 'positive_syphilis' then value
                       end as positivesyphilisduotestehivesifilis,
                   case
                       when rtcli.project = 'newColumn3'
                           and rtcli.outcome = 'unjustified' then value
                       end unjustifiedduotestehivesifilis,
                   case
                       when rtcli.project = 'newColumn4'
                           and rtcli.outcome = 'consumo' then value
                       end as consumehepatitebtestes,
                   case
                       when rtcli.project = 'newColumn4'
                           and rtcli.outcome = 'positive' then value
                       end as positivehepatitebtestes,
                   case
                       when rtcli.project = 'newColumn4'
                           and rtcli.outcome = 'unjustified' then value
                       end unjustifiedhepatitebtestes,
                   case
                       when rtcli.project = 'newColumn5'
                           and rtcli.outcome = 'consumo' then value
                       end as consumetdroraldehiv,
                   case
                       when rtcli.project = 'newColumn5'
                           and rtcli.outcome = 'positive' then value
                       end as positivetdroraldehiv,
                   case
                       when rtcli.project = 'newColumn5'
                           and rtcli.outcome = 'unjustified' then value
                       end unjustifiedtdroraldehiv,
                   case
                       when rtcli.project = 'newColumn6'
                           and rtcli.outcome = 'consumo' then value
                       end as consumenovoteste,
                   case
                       when rtcli.project = 'newColumn6'
                           and rtcli.outcome = 'positive' then value
                       end as positivenovoteste,
                   case
                       when rtcli.project = 'newColumn6'
                           and rtcli.outcome = 'unjustified' then value
                       end unjustifiednovoteste
            from requisition.requisitions r
                     left join referencedata.programs p on
                    r.programid = p.id
                     left join (select *
                                from siglusintegration.rapid_test_consumption_line_items
                                where service <> 'total') rtcli on
                    rtcli.requisitionid = r.id
            where p.code = 'TR') a
      group by a.requisitionid,
               a.service) main
         left join requisition.requisitions r2 on
        r2.id = main.requisitionid
         left join referencedata.processing_periods pp on
        pp.id = r2.processingperiodid
         left join referencedata.facilities f on f.id = r2.facilityid
         LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
         LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
         LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
         LEFT JOIN referencedata.programs p on p.id = r2.programid
where pp.enddate <= now() and p.code = 'TR'
;