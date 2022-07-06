-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP VIEW IF EXISTS dashboard.vw_stock_on_hand_by_lot;
CREATE VIEW dashboard.vw_stock_on_hand_by_lot AS SELECT DISTINCT ON (l.id) f.code AS facilitycode,
    f.name AS facilityname,
    ft.code AS facilitytype,
    gz.code AS districtcode,
    gz.name AS districtname,
    gz_prov.code AS provincecode,
    gz_prov.name AS provincename,
    vfs.districtfacilitycode,
    vfs.provincefacilitycode,
    o.code::text AS productcode,
    o.fullproductname::text AS productname,
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
   FROM stockmanagement.stock_cards sc
     LEFT JOIN referencedata.lots l ON l.id = sc.lotid
     LEFT JOIN referencedata.facilities f ON f.id = sc.facilityid
     LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
     LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
     LEFT JOIN ( SELECT DISTINCT ON (orderables.id) orderables.id,
            orderables.code,
            orderables.fullproductname,
            max(orderables.versionnumber) OVER (PARTITION BY orderables.id) AS max
           FROM referencedata.orderables) o ON o.id = sc.orderableid
     LEFT JOIN ( SELECT cs.stockcardid,
            cs.stockonhand,
            cs.occurreddate,
            cs.processeddate,
            cs.row_number
           FROM ( SELECT calculated_stocks_on_hand.stockcardid,
                    calculated_stocks_on_hand.stockonhand,
                    calculated_stocks_on_hand.occurreddate,
                    max(calculated_stocks_on_hand.processeddate) OVER (PARTITION BY calculated_stocks_on_hand.stockcardid) AS processeddate,
                    row_number() OVER (PARTITION BY calculated_stocks_on_hand.stockcardid ORDER BY calculated_stocks_on_hand.occurreddate DESC) AS row_number
                   FROM stockmanagement.calculated_stocks_on_hand) cs
          WHERE cs.row_number = 1) csoh ON csoh.stockcardid = sc.id
     LEFT JOIN siglusintegration.hf_cmms hc ON hc.facilitycode::text = f.code AND hc.productcode::text = o.code::text
     LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
     LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id;