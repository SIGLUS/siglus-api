-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop view if exists dashboard.vw_tracer_drug;
create view  dashboard.vw_tracer_drug as
select
    case
        when tdpd.stockonhand = 0 then 'Stockout'::text
        when tdpd.cmm = '-1'::integer::double precision
            or tdpd.cmm is null then 'Regular Stock'::text
        when tdpd.stockonhand::double precision < round(tdpd.cmm::numeric, 2) then 'Low Stock'::text
        when prnm.programcode::text = 'TARV'::text
            and (round(cmm::numeric, 2) * 3::double precision) < tdpd.stockonhand::double precision
            then 'Overstock'::text
        when prnm.programcode::text <> 'TARV'::text
            and (round(tdpd.cmm::numeric, 2) * 2::double precision) < tdpd.stockonhand::double precision
            then 'Overstock'::text
        else 'Regular Stock'::text
        end as stockstatus,
    tdpd.computationtime,
    tdpd.productcode,
    o.fullproductname as productname,
    gz_prov.name      as provincename,
    gz.name           as distinctname,
    f.name            as facilityname,
    f.code            as facilitycode,
    vfs.districtfacilitycode,
    vfs.provincefacilitycode
from dashboard.tracer_drug_persistent_data tdpd
         left join (select distinct on
    (id) id,fullproductname,code,
         max(versionnumber) over (partition by id)
                    from  referencedata.orderables) as o on o.code = tdpd.productcode
         left join referencedata.facilities f on tdpd.facilitycode = f.code
         left join referencedata.geographic_zones gz on
        gz.id = f.geographiczoneid
         left join referencedata.geographic_zones gz_prov on
        gz_prov.id = gz.parentid
         left join dashboard.vw_facility_supplier vfs on
        vfs.facilitycode = f.code
         left join stockmanagement.stock_cards sc on
            sc.orderableid = o.id and sc.facilityid = f.id
         left join siglusintegration.program_requisition_name_mapping prnm
                   on prnm.programid = sc.programid