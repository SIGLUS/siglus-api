-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop view if exists dashboard.vw_expiring_products;
CREATE VIEW dashboard.vw_expiring_products
            (provincename, districtname, facilitycode, facilityname, lotnumber, expirydate, productcode, proudctname,
             stockonhand, totalprice, facilitytype, facilitymergetype, cmm, mos, districtfacilitycode,
             provincefacilitycode,
             expireinonemonth)
as
SELECT pgz.name                                                         AS provincename,
       gz.name                                                          AS districtname,
       f.code                                                           AS facilitycode,
       f.name                                                           AS facilityname,
       l.lotcode                                                        AS lotnumber,
       l.expirationdate                                                 AS expirydate,
       o.code                                                           AS productcode,
       o.fullproductname                                                AS proudctname,
       scoh.stockonhand,
       po.priceperpack * scoh.stockonhand                               AS totalprice,
       ft.code                                                          AS facilitytype,
       ftm.category                                                     AS facilitymergetype,
       (CASE
            WHEN hfcmms.cmm IS NULL OR hfcmms.cmm <= 0 THEN 0
            ELSE ROUND(hfcmms.cmm::numeric, 2) END)                              AS cmm,
       (CASE
            WHEN hfcmms.cmm IS NULL OR hfcmms.cmm <= 0 THEN 0
            ELSE ROUND(scoh.stockonhand / ROUND(hfcmms.cmm::numeric, 2), 1) END) AS mos,
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       (CASE
            WHEN l.expirationdate BETWEEN current_date AND current_date + INTERVAL '30 day' THEN 1
            ELSE 0 END)                                                 AS expireinonemonth
FROM referencedata.facilities f
         LEFT JOIN stockmanagement.stock_cards sc ON f.id = sc.facilityid
         LEFT JOIN referencedata.facility_types ft ON ft.id = f.typeid
         LEFT JOIN siglusintegration.facility_type_mapping ftm ON ftm.facilitytypecode = ft.code
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
         LEFT JOIN (SELECT scoh1.id,
                           scoh1.stockonhand,
                           scoh1.occurreddate,
                           scoh1.stockcardid
                    FROM stockmanagement.calculated_stocks_on_hand scoh1
                    WHERE NOT (EXISTS(SELECT 1
                                      FROM stockmanagement.calculated_stocks_on_hand scoh2
                                      WHERE scoh2.stockcardid = scoh1.stockcardid
                                        AND scoh2.occurreddate > scoh1.occurreddate))) scoh
                   ON sc.id = scoh.stockcardid
         LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
         LEFT JOIN referencedata.geographic_zones pgz ON gz.parentid = pgz.id
         LEFT JOIN referencedata.lots l ON l.id = sc.lotid
         LEFT JOIN (SELECT o1.id,
                           o1.fullproductname,
                           o1.code
                    FROM referencedata.orderables o1
                    WHERE NOT (EXISTS(SELECT 1
                                      FROM referencedata.orderables o2
                                      WHERE o2.id = o1.id
                                        AND o2.versionnumber > o1.versionnumber))) o ON sc.orderableid = o.id


         LEFT JOIN (SELECT po1.id,
                           po1.priceperpack,
                           po1.orderableid
                    FROM referencedata.program_orderables po1
                    WHERE NOT (EXISTS(SELECT 1
                                      FROM referencedata.program_orderables po2
                                      WHERE po2.orderableid = po1.orderableid
                                        AND po2.orderableversionnumber > po1.orderableversionnumber))) po
                   ON sc.orderableid = po.orderableid
         LEFT JOIN (SELECT hfcmms1.cmm,
                           hfcmms1.facilitycode,
                           hfcmms1.productcode
                    FROM siglusintegration.hf_cmms hfcmms1
                    WHERE NOT (EXISTS(SELECT 1
                                      FROM siglusintegration.hf_cmms hfcmms2
                                      WHERE hfcmms1.productcode = hfcmms2.productcode
                                        AND hfcmms1.facilitycode = hfcmms2.facilitycode
                                        AND hfcmms1.periodend < hfcmms2.periodend))) hfcmms
                   ON hfcmms.productcode = o.code AND hfcmms.facilitycode = f.code
WHERE l.expirationdate BETWEEN current_date AND current_date + INTERVAL '180 day';