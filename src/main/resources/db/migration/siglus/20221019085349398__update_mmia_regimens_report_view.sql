-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_mmia_regimens_report;
create view dashboard.vw_mmia_regimens_report as
select distinct
        on (rq.id ,rgli.regimenid)
        gz_prov.name as province,
        gz.name as district,
        pp.requisitionperiod as requisitionperiod,
        pp.startdate,
        pp.enddate,
        ft.code as facilitytype,
        f.code as facilitycode,
        f.name as facilityname,
        case
        when rq.emergency = true then 'Emergência'
        else 'Periódica'
end as requisitiontype,
        rgli.value as patients,
        rg.name::text as regimenname,
        re.requisitionnumberprefix::text || lpad(re.requisitionnumber::text, 7, '0') as requisitionnumber,
        p."name" as program,
        ttp.totalpatients as totalpatients,
        ttp.patientsintreatment as patientsintreatment,
        ttp.correctionfactor as correctionfactor,
        rgli.value::numeric * ttp.correctionfactor as patientsadjusted,
        vfs.districtfacilitycode,
        vfs.provincefacilitycode
    from
        requisition.requisitions rq
    left join (
        select
            *
        from
            siglusintegration.regimen_line_items
        where
            columnname = 'patients') rgli on
        rgli.requisitionid = rq.id
    left join (
        select
            rli.requisitionid,
            rli.value as totalpatients,
            pli.value as patientsintreatment,
            case
                when pli.value = 0
                    or rli.value is null
                    or pli.value is null then 0::numeric
                    else round(rli.value::numeric/pli.value::numeric, 2)
                end as correctionfactor
            from
                (
                select
                    *
                from
                    siglusintegration.regimen_line_items
                where
                    regimenid is null
                    and columnname = 'patients') rli
            left join(
                select
                    *
                from
                    siglusintegration.patient_line_items
                where
                    groupname = 'newSection6'
                    and columnname = 'total') pli on
                rli.requisitionid = pli.requisitionid
    ) ttp on
        ttp.requisitionid = rq.id
    left join siglusintegration.requisition_extension re on
        re.requisitionid = rq.id
    left join siglusintegration.regimens rg on
        rg.id = rgli.regimenid
    left join (
        select
            *,
            concat(to_char(startdate::timestamp with time zone, 'DD'::text), ' ',
            case to_char(startdate::timestamp with time zone, 'MM'::text)
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
            end, ' ', to_char(startdate::timestamp with time zone, 'YYYY'::text), ' - ', to_char(enddate::timestamp with time zone, 'DD'::text), ' ',
            case to_char(enddate::timestamp with time zone, 'MM'::text)
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
            end, ' ', to_char(enddate::timestamp with time zone, 'YYYY'::text)) as requisitionperiod
        from
            referencedata.processing_periods) pp on
        pp.id = rq.processingperiodid
    left join referencedata.facilities f on
        f.id = rq.facilityid
    left join referencedata.geographic_zones gz on
        gz.id = f.geographiczoneid
    left join referencedata.geographic_zones gz_prov on
        gz_prov.id = gz.parentid
    left join dashboard.vw_facility_supplier vfs on
        vfs.facilitycode = f.code
    left join referencedata.programs p on
        p.id = rq.programid
    LEFT JOIN referencedata.facility_types ft ON
        f.typeid = ft.id

    where
        rgli.regimenid is not null
        and
        p.code = 'T';