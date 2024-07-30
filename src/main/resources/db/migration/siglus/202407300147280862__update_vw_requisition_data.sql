-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_requisition_data;

CREATE OR REPLACE VIEW dashboard.vw_requisition_data
AS WITH temp_malaria_product_code AS (
    SELECT DISTINCT o_1.code
    FROM referencedata.orderables o_1
    WHERE (o_1.id IN ( SELECT ao.additionalorderableid
                       FROM siglusintegration.program_additional_orderables ao
                                LEFT JOIN referencedata.programs p_1 ON p_1.id = ao.programid
                       WHERE p_1.code::text = 'ML'::text))
), temp_mmc_product_code AS (
    SELECT DISTINCT o_1.code
    FROM referencedata.orderables o_1
    WHERE (o_1.id IN ( SELECT ao.additionalorderableid
                       FROM siglusintegration.program_additional_orderables ao
                                LEFT JOIN referencedata.programs p_1 ON p_1.id = ao.programid
                       WHERE p_1.code::text = 'MMC'::text))
)
   SELECT rli.id AS rliid,
          r.id AS rid,
          r.programid,
          t.orderableid,
          gz_prov.name AS province,
          gz.name AS district,
          ft.code AS facilitytype,
          rf.code AS facilitycode,
          rf.name AS facilityname,
          prnm.reportname,
          pp.enddate AS requisitionperiod,
          concat(to_char(pp.startdate::timestamp with time zone, 'DD'::text), ' ',
                 CASE to_char(pp.startdate::timestamp with time zone, 'MM'::text)
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
                     END, ' ', to_char(pp.startdate::timestamp with time zone, 'YYYY'::text), ' - ', to_char(pp.enddate::timestamp with time zone, 'DD'::text), ' ',
                 CASE to_char(pp.enddate::timestamp with time zone, 'MM'::text)
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
                     END, ' ', to_char(pp.enddate::timestamp with time zone, 'YYYY'::text)) AS originalperiod,
          o.code AS productcode,
          o.fullproductname AS productname,
          rli.beginningbalance AS initialstock,
          rli.totalreceivedquantity AS totalentries,
          CASE
              WHEN (( SELECT count(*) AS count
                      FROM stockmanagement.stock_card_line_items seli
                               LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                               LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                      WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND seli.quantity IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND (sclir.description !~~ '%Loan%'::text OR sclir.description IS NULL) AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate)) > 0
                  THEN (( SELECT
                        CASE
                            WHEN sum(seli.quantity) IS NULL THEN 0::bigint
                            ELSE sum(seli.quantity)
                            END AS sum
                    FROM stockmanagement.stock_card_line_items seli
                             LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                             LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                    WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND (sclir.description !~~ '%Loan%'::text OR sclir.description IS NULL) AND sclir.reasontype = 'CREDIT'::text AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate)) - (
                        ( SELECT
                               CASE
                                   WHEN sum(seli.quantity) IS NULL THEN 0::bigint
                                   ELSE sum(seli.quantity)
                                   END AS sum
                           FROM stockmanagement.stock_card_line_items seli
                                    LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                                    LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                           WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND (sclir.description !~~ '%Loan%'::text OR sclir.description IS NULL) AND sclir.reasontype = 'DEBIT'::text AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate))
              ELSE NULL::bigint
              END AS adjustments,
          CASE
              WHEN (( SELECT count(*) AS count
                      FROM stockmanagement.stock_card_line_items seli
                               LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                               LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                      WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND seli.quantity IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND sclir.description ~~ '%Loan%'::text AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate)) > 0
                  THEN (( SELECT
                        CASE
                            WHEN sum(seli.quantity) IS NULL THEN 0::bigint
                            ELSE sum(seli.quantity)
                            END AS sum
                    FROM stockmanagement.stock_card_line_items seli
                             LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                             LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                    WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND sclir.description ~~ '%Loan%'::text AND sclir.reasontype = 'CREDIT'::text AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate)) - (
                        ( SELECT
                           CASE
                               WHEN sum(seli.quantity) IS NULL THEN 0::bigint
                               ELSE sum(seli.quantity)
                               END AS sum
                       FROM stockmanagement.stock_card_line_items seli
                                LEFT JOIN stockmanagement.stock_cards se ON seli.stockcardid = se.id
                                LEFT JOIN stockmanagement.stock_card_line_item_reasons sclir ON seli.reasonid = sclir.id
                       WHERE seli.sourceid IS NULL AND seli.destinationid IS NULL AND seli.reasonid IS NOT NULL AND sclir.reasoncategory = 'ADJUSTMENT'::text AND sclir.description ~~ '%Loan%'::text AND sclir.reasontype = 'DEBIT'::text AND se.facilityid = r.facilityid AND se.programid = r.programid AND se.orderableid = rli.orderableid AND seli.occurreddate >= pp.startdate AND seli.occurreddate <= pp.enddate))
              ELSE NULL::bigint
              END AS loans,
          rli.totalconsumedquantity AS issues,
          rli.beginningbalance + rli.totalreceivedquantity - rli.totalconsumedquantity AS theoreticalinventory,
          rli.stockonhand AS actualinventory,
          rli.stockonhand::numeric * t.priceperpack AS totalprice,
          vfs.districtfacilitycode,
          vfs.provincefacilitycode,
          r.facilityid,
          pp.id AS periodid,
          pp.name AS periodname,
          CASE
              WHEN ((o.code::text IN ( SELECT temp_malaria_product_code.code
                                       FROM temp_malaria_product_code))) IS TRUE THEN 'Malaria'::character varying
              WHEN ((o.code::text IN ( SELECT temp_mmc_product_code.code
                                       FROM temp_mmc_product_code))) IS TRUE THEN 'Material Medico Cirúrgico'::character varying
              ELSE prnm.programname
              END AS programname
   FROM requisition.requisition_line_items rli
            LEFT JOIN requisition.requisitions r ON r.id = rli.requisitionid
            LEFT JOIN referencedata.facilities rf ON r.facilityid = rf.id
            LEFT JOIN referencedata.geographic_zones gz ON gz.id = rf.geographiczoneid
            LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
            LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = rf.code
            LEFT JOIN referencedata.programs p ON r.programid = p.id
            LEFT JOIN siglusintegration.program_report_name_mapping prnm ON prnm.programid = p.id
            LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id
            LEFT JOIN ( SELECT DISTINCT ON (o_1.id) o_1.id,
                                                    o_1.code,
                                                    o_1.fullproductname,
                                                    o_1.versionnumber
                        FROM referencedata.orderables o_1
                        ORDER BY o_1.id, o_1.versionnumber DESC) o ON rli.orderableid = o.id
            LEFT JOIN referencedata.facility_types ft ON rf.typeid = ft.id
            LEFT JOIN ( SELECT DISTINCT ON (po_1.orderableid) po_1.priceperpack,
                                                              po_1.orderableid
                        FROM referencedata.program_orderables po_1
                        ORDER BY po_1.orderableid, po_1.orderableversionnumber DESC) t ON rli.orderableid = t.orderableid;
