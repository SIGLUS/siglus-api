-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create view dashboard.expiring_product_report
            (provincename, districtname, facilitycode, facilityname, lotnumber, expirydate, productcode, proudctname,
             stockonhand, price, facilitytype, facilitymergetype, cmm, districtfacilitycode, provincefacilitycode,
             withinonemouth)
as
SELECT pgz.name                                                                                     AS provincename,
       gz.name                                                                                      AS districtname,
       f.code                                                                                       AS facilitycode,
       f.name                                                                                       AS facilityname,
       l.lotcode                                                                                    AS lotnumber,
       l.expirationdate                                                                             AS expirydate,
       o.code                                                                                       AS productcode,
       o.fullproductname                                                                            AS proudctname,
       scoh.stockonhand,
       po.priceperpack                                                                              AS price,
       ft.code                                                                                      AS facilitytype,
       ftm.category                                                                                 AS facilitymergetype,
       hfcmms.cmm,
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       (case when l.expirationdate between now() AND now() + INTERVAL '1 months' THEN 1 ELSE 0 END) AS withinonemouth

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
                                        AND scoh2.occurreddate > scoh1.occurreddate))) scoh ON sc.id = scoh.stockcardid
         LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
         LEFT JOIN referencedata.geographic_zones pgz ON gz.parentid = pgz.id
         LEFT JOIN referencedata.lots l ON l.id = sc.lotid
         LEFT JOIN (SELECT DISTINCT ON (orderables.id) orderables.id,
                                                       orderables.fullproductname,
                                                       orderables.code
                    FROM referencedata.orderables) o ON sc.orderableid = o.id
         LEFT JOIN referencedata.program_orderables po ON o.id = po.orderableid
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

where l.expirationdate between now() AND now() + INTERVAL '6 months';



