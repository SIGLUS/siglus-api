CREATE TABLE siglusintegration.program_requisition_name_mapping
(
    id uuid NOT NULL
        PRIMARY KEY,
    programid uuid NOT NULL,
    programcode varchar(255),
    requisitionname varchar(255)
);

INSERT INTO siglusintegration.program_requisition_name_mapping (
    id, programid, programcode, requisitionname
)
VALUES ( 'b9c97279-c6cc-4cda-b9cd-b3147d204e6f',
         (SELECT id FROM referencedata.programs WHERE code = 'TR'),
         (SELECT name FROM referencedata.programs WHERE code = 'TR'),
         'MMIT');
INSERT INTO siglusintegration.program_requisition_name_mapping (
    id, programid, programcode, requisitionname
)
VALUES ( '9308af3a-7b9f-4766-be44-c6be01f94ac7',
         (SELECT id FROM referencedata.programs WHERE code = 'ML'),
         (SELECT name FROM referencedata.programs WHERE code = 'ML'),
         'Malaria');
INSERT INTO siglusintegration.program_requisition_name_mapping (
    id, programid, programcode, requisitionname
)
VALUES ( 'd565b01b-903f-43bb-9c63-9e4aea405c2a',
         (SELECT id FROM referencedata.programs WHERE code = 'T'),
         (SELECT name FROM referencedata.programs WHERE code = 'T'),
         'MMIA');
INSERT INTO siglusintegration.program_requisition_name_mapping (
    id, programid, programcode, requisitionname
)
VALUES ( '936d3ccb-4d09-4c50-9b91-09bb35886640',
         (SELECT id FROM referencedata.programs WHERE code = 'VC'),
         (SELECT name FROM referencedata.programs WHERE code = 'VC'),
         'Balance Requisition');
INSERT INTO siglusintegration.program_requisition_name_mapping (
    id, programid, programcode, requisitionname
)
VALUES ( 'b9c8a001-a7ce-42ab-9e11-4e0f85fab07a',
         (SELECT id FROM referencedata.programs WHERE code = 'TB'),
         (SELECT name FROM referencedata.programs WHERE code = 'TB'),
         'MMTB');

CREATE VIEW dashboard.vw_requisition_monthly_report
            (id, district, province,
             facilityname, ficilitycode, inventorydate, statusdetail, submittedstatus, reporttype, reportname, originalperiod,
             submittedtime,
             synctime,
             facilityid,
             facilitytype, facilitymergetype, districtfacilitycode,
             provincefacilitycode,
             submitteduser)
AS
SELECT r.id                                       id,
       fz.District                                District,
       fz.Province                                Province,
       fz.name                                    ficilityName,
       rf.code                                    ficilityCode,
       r.extradata::json ->> 'actualEndDate'      InventoryDate,
       r.status                                   StatusDetail,
       (CASE
            WHEN r.status IN ('INITIATED', 'SUBMITTED', 'AUTHORIZED') THEN 'Not submitted'
            WHEN r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') THEN (SELECT (CASE
                                                                                                                 WHEN (s.createddate BETWEEN p.startdate AND p.enddate)
                                                                                                                     THEN 'On time'
                                                                                                                 ELSE 'Late' END
                                                                                                                )
                                                                                                     FROM requisition.requisitions rr
                                                                                                              LEFT JOIN requisition.status_changes s ON rr.id = s.requisitionid
                                                                                                              LEFT JOIN referencedata.processing_periods p ON rr.processingperiodid = p.id
                                                                                                     WHERE s.status = 'SUBMITTED'
                                                                                                       AND rr.id = r.id) END
           )                                      SubmittedStatus,
       (CASE
            WHEN r.emergency IS TRUE THEN 'Emergency'
            ELSE 'Regular'
           END)                                   ReportType,
       pnm.requisitionname                        ReportName,
       CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ', (CASE TO_CHAR(pp.startdate, 'MM')
                                                     WHEN '01' THEN 'Jan'
                                                     WHEN '02' THEN 'Fev'
                                                     WHEN '03' THEN 'Mar'
                                                     WHEN '04' THEN 'Abr'
                                                     WHEN '05' THEN 'Mai'
                                                     WHEN '06' THEN 'Jun'
                                                     WHEN '07' THEN 'Jul'
                                                     WHEN '08' THEN 'Ago'
                                                     WHEN '09' THEN 'Set'
                                                     WHEN '10' THEN 'Out'
                                                     WHEN '11' THEN 'Nov'
                                                     WHEN '12' THEN 'Dez' END), ' ',
              TO_CHAR(pp.startdate, 'YYYY'), ' - ', TO_CHAR(pp.enddate, 'DD'), ' ',
              (CASE TO_CHAR(pp.enddate, 'MM')
                   WHEN '01' THEN 'Jan'
                   WHEN '02' THEN 'Fev'
                   WHEN '03' THEN 'Mar'
                   WHEN '04' THEN 'Abr'
                   WHEN '05' THEN 'Mai'
                   WHEN '06' THEN 'Jun'
                   WHEN '07' THEN 'Jul'
                   WHEN '08' THEN 'Ago'
                   WHEN '09' THEN 'Set'
                   WHEN '10' THEN 'Out'
                   WHEN '11' THEN 'Nov'
                   WHEN '12' THEN 'Dez' END), ' ', TO_CHAR(pp.enddate, 'YYYY'))
                                                  OriginalPeriod,
       (CASE
            WHEN r.extradata::json ->> 'clientSubmittedTime' IS NOT NULL
                THEN CAST(r.extradata::json ->> 'clientSubmittedTime' AS timestamp WITH TIME ZONE)
            ELSE CAST(r.createddate AS timestamp WITH TIME ZONE) END) SubmittedTime,
       r.createddate                              SyncTime,
       -- extra info
       r.facilityid                               FacilityId,
       ft.code                                    FacilityType,
       ftm.category                                FacilityMergeType,
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       u.username                                 SubmittedUser
FROM requisition.requisitions r
         LEFT JOIN
     (SELECT f.id, f.name, zz.District District, zz.Province Province
      FROM referencedata.facilities f
               LEFT JOIN
           (SELECT z1.id id, z1.name District, z2.name Province
            FROM referencedata.geographic_zones z1
                     LEFT JOIN referencedata.geographic_zones z2 ON z1.parentid = z2.id) zz
           ON f.geographiczoneid = zz.id) fz
     ON r.facilityid = fz.id
         LEFT JOIN referencedata.programs p ON r.programid = p.id
         LEFT JOIN siglusintegration.program_requisition_name_mapping pnm ON pnm.programid = p.id
         LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id
         LEFT JOIN referencedata.facilities rf ON r.facilityid = rf.id
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = rf.code
         LEFT JOIN referencedata.facility_types ft ON rf.typeid = ft.id
         LEFT JOIN siglusintegration.facility_type_mapping ftm on ftm.facilitytypecode = ft.code
         LEFT JOIN (SELECT DISTINCT FIRST_VALUE(sc.authorid)
                                    OVER (PARTITION BY sc.requisitionid ORDER BY sc.createddate DESC) lastauthorid,
                                    sc.requisitionid
                    FROM requisition.status_changes sc
                    WHERE status = 'SUBMITTED') scf ON r.id = scf.requisitionid
         LEFT JOIN referencedata.users u ON scf.lastauthorid = u.id;

