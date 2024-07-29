-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE OR REPLACE VIEW dashboard.vw_stock_on_hand_by_lot
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
   SELECT f.code AS facilitycode,
          f.name AS facilityname,
          ft.code AS facilitytype,
          gz.code AS districtcode,
          gz.name AS districtname,
          gz_prov.code AS provincecode,
          gz_prov.name AS provincename,
          vfs.districtfacilitycode,
          vfs.provincefacilitycode,
          o.code AS productcode,
          o.fullproductname AS productname,
          CASE
              WHEN ((o.code::text IN ( SELECT temp_malaria_product_code.code
                                       FROM temp_malaria_product_code))) IS TRUE THEN 'Malaria'::text
              WHEN ((o.code::text IN ( SELECT temp_mmc_product_code.code
                                       FROM temp_mmc_product_code))) IS TRUE THEN 'Material Medico Cirúrgico'::text
              ELSE p.name
              END AS programname,
          l.id AS lotid,
          l.lotcode AS lotnumber,
          csoh.stockonhand AS sohoflot,
          l.expirationdate AS expriydate,
          csoh.processeddate AS lastupdatefromtablet,
          hc.cmm AS realcmm,
          CASE
              WHEN hc.cmm IS NULL OR hc.cmm <= 0::double precision THEN 0::numeric
              ELSE round(hc.cmm::numeric, 2)
              END AS cmm,
          CASE
              WHEN hc.cmm IS NULL OR hc.cmm <= 0::double precision THEN 0::numeric
              ELSE round(csoh.stockonhand::numeric / round(hc.cmm::numeric, 2), 1)
              END AS mos
   FROM ( SELECT stock_cards.id,
                 stock_cards.facilityid,
                 stock_cards.lotid,
                 stock_cards.orderableid,
                 stock_cards.programid,
                 stock_cards.origineventid
          FROM stockmanagement.stock_cards
          WHERE stock_cards.lotid IS NOT NULL) sc
            LEFT JOIN referencedata.lots l ON l.id = sc.lotid
            LEFT JOIN referencedata.facilities f ON f.id = sc.facilityid
            LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
            LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
            LEFT JOIN (select distinct on (o_1.id)
                           o_1.id,
                           o_1.code,
                           o_1.fullproductname,
                           o_1.versionnumber
                       from referencedata.orderables o_1
                       order by o_1.id, o_1.versionnumber desc) o ON o.id = sc.orderableid
            LEFT JOIN dashboard.vw_calculated_stocks_on_hand csoh ON csoh.stockcardid = sc.id
            LEFT JOIN ( SELECT DISTINCT ON (hc_1.facilitycode, hc_1.productcode) hc_1.facilitycode,
                                                                                 hc_1.productcode,
                                                                                 first_value(hc_1.cmm) OVER (PARTITION BY hc_1.facilitycode, hc_1.productcode ORDER BY hc_1.periodend DESC) AS cmm
                        FROM siglusintegration.hf_cmms hc_1) hc ON hc.facilitycode::text = f.code AND hc.productcode::text = o.code::text
            LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
            LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
            LEFT JOIN referencedata.programs p ON p.id = sc.programid;
