-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_historical_data;
create view dashboard.vw_historical_data as select
                                                gz_prov.name as province,
                                                gz.name as district,
                                                f.code as facilitycode,
                                                f.name as facilityname,
                                                pp.startdate,
                                                pp.enddate,
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
                                                o.code as productcode,
                                                o.fullproductname as productname,
                                                hdp.*
                                            from
                                                dashboard.historical_data_persistent hdp
                                                    left join referencedata.facilities f on
                                                        f.id = hdp.facilityid
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
                                                        o.id = hdp.orderableid
                                                    left join referencedata.processing_periods pp on
                                                        pp.id = hdp.periodid
                                                    left join referencedata.geographic_zones gz on
                                                        gz.id = f.geographiczoneid
                                                    left join referencedata.geographic_zones gz_prov on
                                                        gz_prov.id = gz.parentid
                                                    left join dashboard.vw_facility_supplier vfs on
                                                        vfs.facilitycode = f.code