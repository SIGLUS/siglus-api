-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_mmtb_regimens;
create view dashboard.vw_mmtb_regimens as
select
    pli.id,
    gz_prov.name as province,
    gz.name as district,
    pp.startdate,
    pp.enddate,
    concat(to_char(pp.startdate::timestamp with time zone, 'DD'::text), ' ',
           case to_char(pp.startdate::timestamp with time zone, 'MM'::text)
               when '01'::text then 'Jan'::text
               when '02'::text then 'Fev'::text
               when '03'::text then 'Mar'::text
               when '04'::text then 'Abr'::text
               when '05'::text then 'Mai'::text
               when '06'::text then 'Jun'::text
               when '07'::text then 'Jul'::text
               when '08'::text then 'Ago'::text
               when '09'::text then 'Set'::text
               when '10'::text then 'Out'::text
               when '11'::text then 'Nov'::text
               when '12'::text then 'Dez'::text
               else null::text
               end, ' ', to_char(pp.startdate::timestamp with time zone, 'YYYY'::text), ' - ', to_char(pp.enddate::timestamp with time zone, 'DD'::text), ' ',
           case to_char(pp.enddate::timestamp with time zone, 'MM'::text)
               when '01'::text then 'Jan'::text
               when '02'::text then 'Fev'::text
               when '03'::text then 'Mar'::text
               when '04'::text then 'Abr'::text
               when '05'::text then 'Mai'::text
               when '06'::text then 'Jun'::text
               when '07'::text then 'Jul'::text
               when '08'::text then 'Ago'::text
               when '09'::text then 'Set'::text
               when '10'::text then 'Out'::text
               when '11'::text then 'Nov'::text
               when '12'::text then 'Dez'::text
               else null::text
               end, ' ', to_char(pp.enddate::timestamp with time zone, 'YYYY'::text)) as requisitionperiod,
    f."name" as facilityname,
    f.code as facilitycode,
    ft.code as facilitytype,
    case
        when r.emergency = true then 'Emergência'::text
        else 'Periódica'::text
        end as requisitiontype,
    re.requisitionnumberprefix::text || lpad(re.requisitionnumber::text, 2, '0') as requisitionnumber,
        pli.value ,
    case
        when pli.groupname = 'newSection2'
            and pli.columnname = 'new' then 'Isoniazida 100 mg'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn0' then 'Isoniazida 300 mg'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn1' then 'Levofloxacina 100 mg Disp'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn2' then 'Levofloxacina 250 mg Rev'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn3' then 'Rifapentina 300 mg + Isoniazida 300 mg'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn4' then 'Rifapentina 150 mg'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn5' then 'Piridoxina 25mg'
        when pli.groupname = 'newSection2'
            and pli.columnname = 'newColumn6' then 'Piridoxina 50mg'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'new' then 'Adulto-Sensível Intensiva'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn6' then 'Adulto-Sensível Manutenção'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn0' then 'Adulto-MR Indução'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn1' then 'Adulto-MR Intensiva'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn2' then 'Adulto-MR Manutenção'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn3' then 'Adulto-XR Indução'
        when pli.groupname = 'newSection3'
            and pli.columnname = 'newColumn4' then 'Adulto-XR Manutenção'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'new' then 'Pediatrico-Sensível Intensiva'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn6' then 'Pediatrico-Sensível Manutenção'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn0' then 'Pediatrico-MR Indução'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn1' then 'Pediatrico-MR Intensiva'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn2' then 'Pediatrico-MR Manutenção'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn3' then 'Pediatrico-XR Manutenção'
        when pli.groupname = 'newSection4'
            and pli.columnname = 'newColumn4' then 'Pediatrico-XR Intensiva'
        end as regimen,
    p.name as program,
    vfs.districtfacilitycode,
    vfs.provincefacilitycode,
    r.status as requisitionstatus
from
    requisition.requisitions r
        left join referencedata.programs p on
            r.programid = p.id
        left join referencedata.facilities f on
            f.id = r.facilityid
        left join referencedata.geographic_zones gz on
            gz.id = f.geographiczoneid
        left join referencedata.geographic_zones gz_prov on
            gz_prov.id = gz.parentid
        left join referencedata.processing_periods pp on
            pp.id = r.processingperiodid
        left join siglusintegration.patient_line_items pli on
            pli.requisitionid = r.id
        left join siglusintegration.requisition_extension re on
            re.requisitionid = r.id
        left join referencedata.facility_types ft on
            f.typeid = ft.id
        left join dashboard.vw_facility_supplier vfs on
            vfs.facilitycode = f.code
where
        p.code = 'TB'
  and pli.groupname in ('newSection2','newSection3','newSection4')
  and r.status in ('APPROVED','RELEASED','RELEASED_WITHOUT_ORDER');