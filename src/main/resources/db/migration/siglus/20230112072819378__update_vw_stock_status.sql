-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP VIEW if EXISTS dashboard.vw_stock_status;
CREATE VIEW dashboard.vw_stock_status AS (
WITH temp_malaria_product_code AS (
        SELECT DISTINCT o_1.code
          FROM referencedata.orderables o_1
         WHERE (o_1.id IN ( SELECT ao.additionalorderableid
                  FROM siglusintegration.program_additional_orderables ao
                    LEFT JOIN referencedata.programs p ON p.id = ao.programid
                 WHERE p.code::text = 'ML'::text))
       ), temp_mmc_product_code AS (
        SELECT DISTINCT o_1.code
          FROM referencedata.orderables o_1
         WHERE (o_1.id IN ( SELECT ao.additionalorderableid
                  FROM siglusintegration.program_additional_orderables ao
                    LEFT JOIN referencedata.programs p ON p.id = ao.programid
                 WHERE p.code::text = 'MMC'::text))
       )
SELECT DISTINCT ON (soh.facilityid, soh.orderableid) soh.facilityid,
   f.name AS facilityname,
   f.code AS facilitycode,
   ft.code AS facilitytype,
   soh.orderableid,
   o.code AS productcode,
   o.fullproductname AS productname,
       CASE
           WHEN ((o.code::text IN ( SELECT temp_malaria_product_code.code
              FROM temp_malaria_product_code))) IS TRUE THEN 'Malaria'::character varying
           WHEN ((o.code::text IN ( SELECT temp_mmc_product_code.code
              FROM temp_mmc_product_code))) IS TRUE THEN 'Material Medico Cirúrgico'::character varying
           ELSE prnm.programname
       END AS programname,
   gz_prov.name AS provincename,
   gz.name AS districtname,
   vfs.districtfacilitycode,
   vfs.provincefacilitycode,
   soh.totalsoh,
   soh.totalsoh::numeric * po.priceperpack AS totalprice,
   h.cmm AS realcmm,
       CASE
           WHEN h.cmm IS NULL OR h.cmm <= 0::double precision THEN 0::numeric
           ELSE round(h.cmm::numeric, 2)
       END AS cmm,
       CASE
           WHEN h.cmm IS NULL OR h.cmm <= 0::double precision THEN 0::numeric
           ELSE round(soh.totalsoh::numeric / round(h.cmm::numeric, 2), 1)
       END AS mos,
       CASE
           WHEN soh.totalsoh = 0 THEN 'Roptura de stock'::text
           WHEN h.cmm = '-1'::integer::double precision OR h.cmm IS NULL THEN 'Stock regular'::text
           WHEN soh.totalsoh::double precision < h.cmm THEN 'Eminência de roptura'::text
           WHEN prnm.programname::text = 'TARV'::text AND (h.cmm * 3::double precision) < soh.totalsoh::double precision THEN 'Stock acumulado'::text
           WHEN prnm.programname::text <> 'TARV'::text AND (h.cmm * 2::double precision) < soh.totalsoh::double precision THEN 'Stock acumulado'::text
           ELSE 'Stock regular'::text
       END AS stockstatus,
   csoh.expirationdate AS earlistdrugexpriydate,
   csoh.processeddate AS lastupdatetime
  FROM ( SELECT soh_1.facilityid,
           soh_1.orderableid,
           sum(soh_1.stockonhand) AS totalsoh
          FROM ( SELECT sc_1.facilityid,
                   sc_1.orderableid,
                   csoh_1.stockonhand,
                   csoh_1.occurreddate,
                   row_number() OVER (PARTITION BY sc_1.id ORDER BY csoh_1.occurreddate DESC) AS row_number
                  FROM stockmanagement.calculated_stocks_on_hand csoh_1
                    LEFT JOIN stockmanagement.stock_cards sc_1 ON sc_1.id = csoh_1.stockcardid) soh_1
         WHERE soh_1.row_number = 1
         GROUP BY soh_1.facilityid, soh_1.orderableid) soh
    LEFT JOIN ( SELECT facilityid, orderableid, stockonhand, lotcode, expirationdate, processeddate
                FROM
                    ( SELECT sc_1.facilityid, sc_1.orderableid, csoh_1.stockonhand, csoh_1.occurreddate, csoh_1.processeddate, l.lotcode, l.expirationdate,
                             row_number() OVER (PARTITION BY csoh_1.stockcardid ORDER BY csoh_1.occurreddate DESC) AS row_number
                      FROM stockmanagement.calculated_stocks_on_hand csoh_1
                      LEFT JOIN stockmanagement.stock_cards sc_1 ON csoh_1.stockcardid = sc_1.id
                      LEFT JOIN referencedata.lots l ON sc_1.lotid = l.id
                    ) soh_result
                WHERE soh_result.row_number = 1 AND soh_result.stockonhand != 0) csoh ON csoh.facilityid = soh.facilityid AND csoh.orderableid = soh.orderableid
    LEFT JOIN stockmanagement.stock_cards sc ON sc.facilityid = soh.facilityid AND sc.orderableid = soh.orderableid
    LEFT JOIN siglusintegration.program_report_name_mapping prnm ON prnm.programid = sc.programid
    LEFT JOIN referencedata.facilities f ON f.id = soh.facilityid
    LEFT JOIN ( SELECT o_1.id,
           o_1.code,
           o_1.fullproductname,
           o_1.versionnumber,
           o_1.latestversion
          FROM ( SELECT orderables.id,
                   orderables.code,
                   orderables.fullproductname,
                   orderables.versionnumber,
                   max(orderables.versionnumber) OVER (PARTITION BY orderables.id) AS latestversion
                  FROM referencedata.orderables) o_1
         WHERE o_1.versionnumber = o_1.latestversion) o ON o.id = soh.orderableid
    LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
    LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
    LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
    LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
    LEFT JOIN ( SELECT DISTINCT ON (hc.facilitycode, hc.productcode) hc.facilitycode,
           hc.productcode,
           first_value(hc.cmm) OVER (PARTITION BY hc.facilitycode, hc.productcode ORDER BY hc.periodend DESC) AS cmm
          FROM siglusintegration.hf_cmms hc) h ON h.facilitycode::text = f.code AND h.productcode::text = o.code::text
    LEFT JOIN ( SELECT program_orderables.id,
           program_orderables.active,
           program_orderables.displayorder,
           program_orderables.dosesperpatient,
           program_orderables.fullsupply,
           program_orderables.priceperpack,
           program_orderables.orderabledisplaycategoryid,
           program_orderables.orderableid,
           program_orderables.programid,
           program_orderables.orderableversionnumber
          FROM referencedata.program_orderables
         WHERE ((program_orderables.orderableid, program_orderables.orderableversionnumber) IN ( SELECT program_orderables_1.orderableid,
                   max(program_orderables_1.orderableversionnumber) AS max
                  FROM referencedata.program_orderables program_orderables_1
                 GROUP BY program_orderables_1.orderableid))) po ON po.orderableid = soh.orderableid
);
