-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
ALTER TABLE siglusintegration.program_requisition_name_mapping RENAME programcode TO programname;
ALTER TABLE siglusintegration.program_requisition_name_mapping RENAME requisitionname TO reportname;
ALTER TABLE siglusintegration.program_requisition_name_mapping RENAME TO program_report_name_mapping;

DROP VIEW IF EXISTS dashboard.vw_requisition_data;
CREATE VIEW dashboard.vw_requisition_data AS
SELECT rli.id                                                                         AS rliid,
       r.id                                                                           AS rid,
       r.programid,
       t.orderableid,
       fz.Province                                                                    AS province,
       fz.District                                                                    AS district,
       ft.code                                                                        AS facilitytype,
       rf.code                                                                        AS facilitycode,
       fz.name                                                                        AS facilityname,
       prnm.reportname                                                           AS reportname,
       pp.enddate                                                                     AS requisitionperiod,
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
                   WHEN '12' THEN 'Dez' END), ' ', TO_CHAR(pp.enddate, 'YYYY'))       AS
                                                                                         originalperiod,
       o.code                                                                         AS productcode,
       o.fullproductname                                                              AS productname,
       rli.beginningbalance                                                           AS initialstock,
       rli.totalreceivedquantity                                                      AS totalentries,
       (CASE
            WHEN (SELECT COUNT(*)
                  FROM stockmanagement.stock_card_line_items seli
                           LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                           LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                  WHERE seli.sourceid IS NULL
                    AND seli.destinationid IS NULL
                    AND seli.reasonid IS NOT NULL
                    AND seli.quantity IS NOT NULL
                    AND sclir.reasoncategory = 'ADJUSTMENT'
                    AND (sclir.description NOT LIKE '%Loan%' OR sclir.description IS NULL)
                    AND se.facilityid = r.facilityid
                    AND se.programid = r.programid
                    AND se.orderableid = rli.orderableid
                    AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate) > 0 THEN (
                    (SELECT (CASE WHEN SUM(seli.quantity) IS NULL THEN 0 ELSE SUM(seli.quantity) END)
                     FROM stockmanagement.stock_card_line_items seli
                              LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                              LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                     WHERE seli.sourceid IS NULL
                       AND seli.destinationid IS NULL
                       AND seli.reasonid IS NOT NULL
                       AND sclir.reasoncategory = 'ADJUSTMENT'
                       AND (sclir.description NOT LIKE '%Loan%' OR sclir.description IS NULL)
                       AND sclir.reasontype = 'CREDIT'
                       AND se.facilityid = r.facilityid
                       AND se.programid = r.programid
                       AND se.orderableid = rli.orderableid
                       AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate)
                    -
                    (SELECT (CASE WHEN SUM(seli.quantity) IS NULL THEN 0 ELSE SUM(seli.quantity) END)
                     FROM stockmanagement.stock_card_line_items seli
                              LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                              LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir
                                        ON seli.reasonid = sclir.id
                     WHERE seli.sourceid IS NULL
                       AND seli.destinationid IS NULL
                       AND seli.reasonid IS NOT NULL
                       AND sclir.reasoncategory = 'ADJUSTMENT'
                       AND (sclir.description NOT LIKE '%Loan%' OR sclir.description IS NULL)
                       AND sclir.reasontype = 'DEBIT'
                       AND se.facilityid = r.facilityid
                       AND se.programid = r.programid
                       AND se.orderableid = rli.orderableid
                       AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate)) END)
                                                                                      AS adjustments,
       (CASE
            WHEN (SELECT COUNT(*)
                  FROM stockmanagement.stock_card_line_items seli
                           LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                           LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                  WHERE seli.sourceid IS NULL
                    AND seli.destinationid IS NULL
                    AND seli.reasonid IS NOT NULL
                    AND seli.quantity IS NOT NULL
                    AND sclir.reasoncategory = 'ADJUSTMENT'
                    AND sclir.description LIKE '%Loan%'
                    AND se.facilityid = r.facilityid
                    AND se.programid = r.programid
                    AND se.orderableid = rli.orderableid
                    AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate) > 0 THEN ((
                    (SELECT (CASE WHEN SUM(seli.quantity) IS NULL THEN 0 ELSE SUM(seli.quantity) END)
                     FROM stockmanagement.stock_card_line_items seli
                              LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                              LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                     WHERE seli.sourceid IS NULL
                       AND seli.destinationid IS NULL
                       AND seli.reasonid IS NOT NULL
                       AND sclir.reasoncategory = 'ADJUSTMENT'
                       AND sclir.description LIKE '%Loan%'
                       AND sclir.reasontype = 'CREDIT'
                       AND se.facilityid = r.facilityid
                       AND se.programid = r.programid
                       AND se.orderableid = rli.orderableid
                       AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate)
                    -
                    (SELECT (CASE WHEN SUM(seli.quantity) IS NULL THEN 0 ELSE SUM(seli.quantity) END)
                     FROM stockmanagement.stock_card_line_items seli
                              LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                              LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                     WHERE seli.sourceid IS NULL
                       AND seli.destinationid IS NULL
                       AND seli.reasonid IS NOT NULL
                       AND sclir.reasoncategory = 'ADJUSTMENT'
                       AND sclir.description LIKE '%Loan%'
                       AND sclir.reasontype = 'DEBIT'
                       AND se.facilityid = r.facilityid
                       AND se.programid = r.programid
                       AND se.orderableid = rli.orderableid
                       AND seli.occurreddate BETWEEN pp.startdate AND pp.enddate))) END)
                                                                                      AS loans,
       rli.totalconsumedquantity                                                      AS issues,
       (rli.beginningbalance + rli.totalreceivedquantity - rli.totalconsumedquantity) AS theoreticalinventory,
       rli.stockonhand                                                                AS actualinventory,
       rli.stockonhand * t.priceperpack                                               AS totalprice,
       -- extra
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       r.facilityid,
       pp.id                                                                          AS periodid
FROM requisition.requisition_line_items rli
         LEFT JOIN requisition.requisitions r ON r.id = rli.requisitionid
         LEFT JOIN (SELECT f.id, f.name, zz.District District, zz.Province Province
                    FROM referencedata.facilities f
                             LEFT JOIN
                         (SELECT z1.id id, z1.name District, z2.name Province
                          FROM referencedata.geographic_zones z1
                                   LEFT JOIN referencedata.geographic_zones z2 ON z1.parentid = z2.id) zz
                         ON f.geographiczoneid = zz.id) fz ON r.facilityid = fz.id
         LEFT JOIN referencedata.facilities rf ON r.facilityid = rf.id
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = rf.code
         LEFT JOIN referencedata.programs p ON r.programid = p.id
         LEFT JOIN siglusintegration.program_report_name_mapping prnm ON
        prnm.programid = p.id
         LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id
         LEFT JOIN (SELECT DISTINCT ON
    (orderables.id) orderables.id,
                    FIRST_VALUE(orderables.code)
                    OVER (PARTITION BY orderables.id ORDER BY orderables.versionnumber DESC) AS code,
                    FIRST_VALUE(orderables.fullproductname)
                    OVER (PARTITION BY orderables.id ORDER BY orderables.versionnumber DESC) AS fullproductname
                    FROM referencedata.orderables) o ON rli.orderableid = o.id
         LEFT JOIN referencedata.facility_types ft ON rf.typeid = ft.id
         LEFT JOIN (SELECT DISTINCT FIRST_VALUE(po.priceperpack)
                                    OVER (PARTITION BY po.orderableid, po.programid ORDER BY po.orderableversionnumber DESC) priceperpack
                                  , po.orderableid
                                  , po.programid
                    FROM referencedata.program_orderables po) t
                   ON rli.orderableid = t.orderableid AND r.programid = t.programid;

DROP VIEW IF EXISTS dashboard.vw_requisition_monthly_report;
CREATE VIEW dashboard.vw_requisition_monthly_report
            (id, programid, processingperiodid, district, province, requisitionperiod, facilityname, ficilitycode,
             inventorydate, statusdetail, submittedstatus,
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
            WHEN r.status IN ('INITIATED', 'SUBMITTED', 'AUTHORIZED', 'REJECTED') THEN 'Não Submetido'
            WHEN r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') THEN (
                SELECT (CASE
                            WHEN ((CASE
                                       WHEN r.extradata::json ->> 'clientSubmittedTime' IS NOT NULL
                                           THEN CAST(r.extradata::json ->> 'clientSubmittedTime' AS date)
                                       ELSE CAST(scfc.lastcreateddate AS date) END) BETWEEN p.submitstartdate AND p.submitenddate)
                                THEN 'A tempo'
                            ELSE 'Tarde' END
                           )
                FROM requisition.requisitions rr
                         LEFT JOIN siglusintegration.processing_period_extension  p ON rr.processingperiodid = p.processingperiodid
                WHERE rr.id = r.id) END
           )                                      submittedstatus,
       (CASE
            WHEN r.emergency IS TRUE THEN 'Emergência'
            ELSE 'Regular'
           END)                                   reporttype,
       pnm.reportname as reportname,
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
         LEFT JOIN siglusintegration.program_report_name_mapping pnm ON pnm.programid = p.id
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
       'Não Submetido' as submittedstatus,
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
