-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop view if exists dashboard.vw_mmia_regimens;
CREATE VIEW dashboard.vw_mmia_regimens AS  SELECT DISTINCT ON (rq.id, rgli.regimenid) gz_prov.name AS province,
    gz.name AS district,
    pp.requisitionperiod,
    pp.startdate,
    pp.enddate,
    ft.code AS facilitytype,
    f.code AS facilitycode,
    f.name AS facilityname,
        CASE
            WHEN rq.emergency = true THEN 'Emergência'::text
            ELSE 'Periódica'::text
        END AS requisitiontype,
    rgli.value AS patients,
    rg.name::text AS regimenname,
    re.requisitionnumberprefix::text || lpad(re.requisitionnumber::text, 2, '0'::text) AS requisitionnumber,
    p.name AS program,
    ttp.totalpatients,
    ttp.patientsintreatment,
    ttp.correctionfactor,
    rgli.value::numeric * ttp.correctionfactor AS patientsadjusted,
    vfs.districtfacilitycode,
    vfs.provincefacilitycode,
    rq.status AS requisitionstatus
   FROM requisition.requisitions rq
     LEFT JOIN ( SELECT regimen_line_items.id,
            regimen_line_items.requisitionid,
            regimen_line_items.regimenid,
            regimen_line_items.columnname,
            regimen_line_items.value
           FROM siglusintegration.regimen_line_items
          WHERE regimen_line_items.columnname::text = 'patients'::text) rgli ON rgli.requisitionid = rq.id
     LEFT JOIN (
        SELECT distinct pli.requisitionid, tps.totalpatients, pit.patientsintreatment,
        CASE
            WHEN tps.totalpatients = 0 OR tps.totalpatients IS NULL OR pit.patientsintreatment IS NULL THEN 0::numeric
            ELSE round(pit.patientsintreatment / tps.totalpatients, 2)
        END AS correctionfactor
        FROM siglusintegration.patient_line_items pli
        left join
        (
            select requisitionid, sum(value::numeric) as totalpatients from siglusintegration.patient_line_items where
            (
                (groupname::text = 'newSection2' and columnname::text = 'newColumn3') or
                (groupname::text = 'newSection3' and columnname::text = 'newColumn1') or
                (groupname::text = 'newSection4' and columnname::text = 'new')
            ) group by requisitionid
        ) as tps on pli.requisitionid = tps.requisitionid
        left join
        (
            select requisitionid, sum(value::numeric) as patientsintreatment from siglusintegration.patient_line_items where
            (
                (groupname::text = 'newSection2' and columnname::text = 'total') or
                (groupname::text = 'newSection3' and columnname::text = 'total') or
                (groupname::text = 'newSection4' and columnname::text = 'total')
            ) group by requisitionid
        ) as pit on pli.requisitionid = pit.requisitionid
      ) ttp ON ttp.requisitionid = rq.id
     LEFT JOIN siglusintegration.requisition_extension re ON re.requisitionid = rq.id
     LEFT JOIN siglusintegration.regimens rg ON rg.id = rgli.regimenid
     LEFT JOIN ( SELECT processing_periods.id,
            processing_periods.description,
            processing_periods.enddate,
            processing_periods.name,
            processing_periods.startdate,
            processing_periods.processingscheduleid,
            processing_periods.extradata,
            concat(to_char(processing_periods.startdate::timestamp with time zone, 'DD'::text), ' ',
                CASE to_char(processing_periods.startdate::timestamp with time zone, 'MM'::text)
                    WHEN '01'::text THEN 'Jan'::text
                    WHEN '02'::text THEN 'Fev'::text
                    WHEN '03'::text THEN 'Mar'::text
                    WHEN '04'::text THEN 'Abr'::text
                    WHEN '05'::text THEN 'Mai'::text
                    WHEN '06'::text THEN 'Jun'::text
                    WHEN '07'::text THEN 'Jul'::text
                    WHEN '08'::text THEN 'Ago'::text
                    WHEN '09'::text THEN 'Set'::text
                    WHEN '10'::text THEN 'Out'::text
                    WHEN '11'::text THEN 'Nov'::text
                    WHEN '12'::text THEN 'Dez'::text
                    ELSE NULL::text
                END, ' ', to_char(processing_periods.startdate::timestamp with time zone, 'YYYY'::text), ' - ', to_char(processing_periods.enddate::timestamp with time zone, 'DD'::text), ' ',
                CASE to_char(processing_periods.enddate::timestamp with time zone, 'MM'::text)
                    WHEN '01'::text THEN 'Jan'::text
                    WHEN '02'::text THEN 'Fev'::text
                    WHEN '03'::text THEN 'Mar'::text
                    WHEN '04'::text THEN 'Abr'::text
                    WHEN '05'::text THEN 'Mai'::text
                    WHEN '06'::text THEN 'Jun'::text
                    WHEN '07'::text THEN 'Jul'::text
                    WHEN '08'::text THEN 'Ago'::text
                    WHEN '09'::text THEN 'Set'::text
                    WHEN '10'::text THEN 'Out'::text
                    WHEN '11'::text THEN 'Nov'::text
                    WHEN '12'::text THEN 'Dez'::text
                    ELSE NULL::text
                END, ' ', to_char(processing_periods.enddate::timestamp with time zone, 'YYYY'::text)) AS requisitionperiod
           FROM referencedata.processing_periods) pp ON pp.id = rq.processingperiodid
     LEFT JOIN referencedata.facilities f ON f.id = rq.facilityid
     LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
     LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
     LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
     LEFT JOIN referencedata.programs p ON p.id = rq.programid
     LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
  WHERE rgli.regimenid IS NOT NULL AND p.code::text = 'T'::text AND (rq.status::text = ANY (ARRAY['APPROVED'::character varying::text, 'RELEASED'::character varying::text, 'RELEASED_WITHOUT_ORDER'::character varying::text]));
