-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
-- dashboard.vw_expired_products source

CREATE OR REPLACE VIEW dashboard.vw_expired_products
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
   SELECT pgz.name AS provincename,
          gz.name AS districtname,
          f.code AS facilitycode,
          f.name AS facilityname,
          l.lotcode AS lotnumber,
          l.expirationdate AS expirydate,
          o.code AS productcode,
          o.fullproductname AS proudctname,
          scoh.stockonhand,
          po.priceperpack * scoh.stockonhand::numeric AS totalprice,
          ft.code AS facilitytype,
          ftm.category AS facilitymergetype,
          CASE
              WHEN hfcmms.cmm IS NULL OR hfcmms.cmm <= 0::double precision THEN 0::numeric
              ELSE round(hfcmms.cmm::numeric, 2)
              END AS cmm,
          CASE
              WHEN hfcmms.cmm IS NULL OR hfcmms.cmm <= 0::double precision THEN 0::numeric
              ELSE round(scoh.stockonhand::numeric / round(hfcmms.cmm::numeric, 2), 1)
              END AS mos,
          vfs.districtfacilitycode,
          vfs.provincefacilitycode,
          CASE
              WHEN l.expirationdate < (CURRENT_DATE - '30 days'::interval) THEN 1
              ELSE 0
              END AS hasexpiredformorethanonemonth,
          CASE
              WHEN ((o.code::text IN ( SELECT temp_malaria_product_code.code
                                       FROM temp_malaria_product_code))) IS TRUE THEN 'Malaria'::text
              WHEN ((o.code::text IN ( SELECT temp_mmc_product_code.code
                                       FROM temp_mmc_product_code))) IS TRUE THEN 'Material Medico Cirúrgico'::text
              ELSE p.name
              END AS programname
   FROM referencedata.facilities f
            LEFT JOIN stockmanagement.stock_cards sc ON f.id = sc.facilityid
            LEFT JOIN referencedata.facility_types ft ON ft.id = f.typeid
            LEFT JOIN siglusintegration.facility_type_mapping ftm ON ftm.facilitytypecode = ft.code
            LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
            LEFT JOIN dashboard.vw_calculated_stocks_on_hand scoh ON sc.id = scoh.stockcardid
            LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
            LEFT JOIN referencedata.geographic_zones pgz ON gz.parentid = pgz.id
            LEFT JOIN referencedata.lots l ON l.id = sc.lotid
            LEFT JOIN ( SELECT DISTINCT ON (o_1.id) o_1.id,
                                                    o_1.code,
                                                    o_1.fullproductname,
                                                    o_1.versionnumber
                        FROM referencedata.orderables o_1
                        ORDER BY o_1.id, o_1.versionnumber DESC) o ON sc.orderableid = o.id
            LEFT JOIN ( SELECT DISTINCT ON (po_1.orderableid) po_1.priceperpack,
                                                              po_1.orderableid
                        FROM referencedata.program_orderables po_1
                        ORDER BY po_1.orderableid, po_1.orderableversionnumber DESC) po ON sc.orderableid = po.orderableid
            LEFT JOIN ( SELECT DISTINCT ON (hc_1.facilitycode, hc_1.productcode) hc_1.facilitycode,
                                                                                 hc_1.productcode,
                                                                                 hc_1.cmm
                        FROM siglusintegration.hf_cmms hc_1
                        ORDER BY hc_1.facilitycode, hc_1.productcode, hc_1.periodend DESC) hfcmms ON hfcmms.productcode::text = o.code::text AND hfcmms.facilitycode::text = f.code
            LEFT JOIN referencedata.programs p ON p.id = sc.programid
   WHERE l.expirationdate < CURRENT_DATE AND scoh.stockonhand > 0;
