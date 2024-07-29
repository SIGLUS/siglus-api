-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE OR REPLACE VIEW dashboard.vw_system_update
AS SELECT fac.code AS facilitycode,
          fac.name AS facilityname,
          ftm.category AS facilitymergetype,
          ftm.facilitytypecode AS facilitytype,
          gz_prov.code AS provincecode,
          gz_prov.name AS provincename,
          gz.code AS districtcode,
          gz.name AS districtname,
          vfs.districtfacilitycode,
          vfs.provincefacilitycode,
          r.latestupdatetime,
          CASE
              WHEN r.latestupdatetime >= (now() - '1 day'::interval) THEN 'Actualizaram nas últimas 24 horas'::text
              WHEN r.latestupdatetime < (now() - '1 day'::interval) AND r.latestupdatetime >= (now() - '3 days'::interval) THEN 'Actualizaram nos últimos 3 dias'::text
              WHEN r.latestupdatetime < (now() - '3 days'::interval) THEN 'Actualizaram a mais de 3 dias'::text
              ELSE 'null'::text
              END AS updatestatus
   FROM ( SELECT sc_1.facilityid, max(csoh.processeddate) AS latestupdatetime
          FROM stockmanagement.stock_cards sc_1
                   LEFT JOIN dashboard.vw_calculated_stocks_on_hand csoh ON csoh.stockcardid = sc_1.id
          GROUP BY sc_1.facilityid) r
            LEFT JOIN referencedata.facilities fac ON fac.id = r.facilityid
            LEFT JOIN referencedata.geographic_zones gz ON gz.id = fac.geographiczoneid
            LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
            LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = fac.code
            LEFT JOIN referencedata.facility_types ft ON ft.id = fac.typeid
            LEFT JOIN siglusintegration.facility_type_mapping ftm ON ftm.facilitytypecode = ft.code
   WHERE ftm.category IS NOT NULL;
