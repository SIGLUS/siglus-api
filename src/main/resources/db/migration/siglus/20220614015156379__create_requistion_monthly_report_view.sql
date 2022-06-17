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
            (id, district, province, facilityname, ficilitycode, inventorydate, statusdetail, submittedstatus,
             reporttype, reportname, originalperiod, submittedtime, synctime, facilityid, facilitytype,
             facilitymergetype, districtfacilitycode, provincefacilitycode, submitteduser, clientSubmittedTime,
             requisitionCreateddate, statusLastcreateddate, submitstartdate, submitenddate)
AS
SELECT r.id                                       id,
       fz.District                                district,
       fz.Province                                province,
       fz.name                                    ficilityname,
       rf.code                                    ficilitycode,
       r.extradata::json ->> 'actualEndDate'      inventorydate,
       r.status                                   statusdetail,
       (CASE
            WHEN r.status IN ('INITIATED', 'SUBMITTED', 'AUTHORIZED', 'REJECTED') THEN 'Not submitted'
            WHEN r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') THEN (SELECT (CASE
                                                                                                                 WHEN ((CASE
                                                                                                                            WHEN r.extradata::json ->> 'clientSubmittedTime' IS NOT NULL
                                                                                                                                THEN CAST(r.extradata::json ->> 'clientSubmittedTime' AS date)
                                                                                                                            ELSE CAST(scfc.lastcreateddate AS date) END) BETWEEN p.submitstartdate AND p.submitenddate)
                                                                                                                     THEN 'On time'
                                                                                                                 ELSE 'Late' END
                                                                                                                )
                                                                                                     FROM requisition.requisitions rr
                                                                                                              LEFT JOIN requisition.status_changes s ON rr.id = s.requisitionid
                                                                                                              LEFT JOIN siglusintegration.processing_period_extension  p ON rr.processingperiodid = p.processingperiodid
                                                                                                     WHERE s.status = 'SUBMITTED'
                                                                                                       AND rr.id = r.id) END
           )                                      submittedstatus,
       (CASE
            WHEN r.emergency IS TRUE THEN 'Emergency'
            ELSE 'Regular'
           END)                                   reporttype,
       pnm.requisitionname                        reportname,
       CONCAT(TO_CHAR(ppe.submitstartdate, 'DD'), ' ', (CASE TO_CHAR(ppe.submitstartdate, 'MM')
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
              TO_CHAR(ppe.submitstartdate, 'YYYY'), ' - ', TO_CHAR(ppe.submitenddate, 'DD'), ' ',
              (CASE TO_CHAR(ppe.submitenddate, 'MM')
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
                   WHEN '12' THEN 'Dez' END), ' ', TO_CHAR(ppe.submitenddate, 'YYYY'))
                                                  originalperiod,
       (CASE
            WHEN r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') THEN (CASE
                                                                                                         WHEN r.extradata::json ->> 'clientSubmittedTime' IS NOT NULL
                                                                                                             THEN CAST(r.extradata::json ->> 'clientSubmittedTime' AS TIMESTAMP WITH TIME ZONE)
                                                                                                         ELSE scfc.lastcreateddate END) END) submittedtime,
       (CASE
            WHEN r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') THEN (CASE
                                                                                                         WHEN r.extradata::json ->> 'clientSubmittedTime' IS NOT NULL
                                                                                                             THEN r.createddate
                                                                                                         ELSE scfc.lastcreateddate END) END) synctime,

       -- extra info
       r.facilityid                                facilityid,
       ft.code                                     facilitytype,
       ftm.category                                facilitymergetype,
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       u.username                                  submitteduser,
       r.extradata::json ->> 'clientSubmittedTime' clientsubmittedtime,
       r.createddate                               requisitioncreateddate,
       scfc.lastcreateddate                        statuslastcreateddate,
       ppe.submitstartdate,
       ppe.submitenddate
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
         LEFT JOIN siglusintegration.processing_period_extension ppe ON r.processingperiodid = ppe.processingperiodid
         LEFT JOIN referencedata.facilities rf ON r.facilityid = rf.id
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = rf.code
         LEFT JOIN referencedata.facility_types ft ON rf.typeid = ft.id
         LEFT JOIN siglusintegration.facility_type_mapping ftm ON ftm.facilitytypecode = ft.code
         LEFT JOIN (SELECT DISTINCT MAX(sc.createddate)
                                    OVER (PARTITION BY sc.requisitionid) lastcreateddate,
                                    sc.requisitionid
                    FROM requisition.status_changes sc
                    WHERE status = 'IN_APPROVAL') scfc ON r.id = scfc.requisitionid
         LEFT JOIN (SELECT DISTINCT FIRST_VALUE(sc.authorid)
                                    OVER (PARTITION BY sc.requisitionid ORDER BY sc.createddate DESC) lastauthorid,
                                    sc.requisitionid
                    FROM requisition.status_changes sc
                    WHERE status = 'SUBMITTED') scfa ON r.id = scfa.requisitionid
         LEFT JOIN referencedata.users u ON scfa.lastauthorid = u.id

