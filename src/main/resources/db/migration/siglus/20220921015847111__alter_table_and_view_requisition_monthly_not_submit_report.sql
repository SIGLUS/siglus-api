-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.requisition_monthly_not_submit_report SET SCHEMA dashboard;
ALTER TABLE dashboard.requisition_monthly_not_submit_report ADD PRIMARY KEY (id);

DROP VIEW IF EXISTS dashboard.vw_requisition_monthly_report;
CREATE VIEW dashboard.vw_requisition_monthly_report
            (id
                ,programid, processingperiodid
                , district, province, requisitionperiod, facilityname, ficilitycode, inventorydate, statusdetail, submittedstatus,
             reporttype, reportname, originalperiod, submittedtime, synctime, facilityid, facilitytype,
             facilitymergetype, districtfacilitycode, provincefacilitycode, submitteduser, clientSubmittedTime,
             requisitionCreateddate, statusLastcreateddate, submitstartdate, submitenddate)
AS
SELECT r.id                                       id,
       p.id                                       programid,
       pp.id                                      processingperiodid,
       fz.District                                district,
       fz.Province                                province,
       pp.enddate                                 requisitionperiod,
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
         LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id
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
UNION ALL
SELECT id,
       programid,
       processingperiodid,
       district,
       province,
       requisitionperiod,
       ficilityname,
       ficilitycode,
       inventorydate,
       statusdetail,
       submittedstatus,
       reporttype,
       reportname,
       originalperiod,
       submittedtime,
       synctime,
       facilityid,
       facilitytype,
       facilitymergetype,
       districtfacilitycode,
       provincefacilitycode,
       submitteduser,
       clientsubmittedtime,
       requisitioncreateddate,
       statuslastcreateddate,
       submitstartdate,
       submitenddate
FROM dashboard.requisition_monthly_not_submit_report;