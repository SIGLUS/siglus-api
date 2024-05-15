-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
DROP VIEW if EXISTS dashboard.vw_stock_status;
CREATE OR REPLACE VIEW dashboard.vw_stock_status
AS WITH temp_malaria_product_code AS (
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
), temp_stack_data AS (
    SELECT sc_1.facilityid,
           sc_1.programid,
           sc_1.orderableid,
           sum(csoh_1.stockonhand) AS totalsoh,
           max(csoh_2.processeddate) AS processeddate,
           min(l.expirationdate) AS expirationdate
    FROM stockmanagement.stock_cards sc_1
             LEFT JOIN ( SELECT DISTINCT ON (csoh.stockcardid) csoh.stockcardid,
                                                               csoh.stockonhand,
                                                               csoh.occurreddate
                         FROM stockmanagement.calculated_stocks_on_hand csoh
                         ORDER BY csoh.stockcardid, csoh.occurreddate DESC) csoh_1 ON csoh_1.stockcardid = sc_1.id
             LEFT JOIN ( SELECT DISTINCT ON (csoh.stockcardid) csoh.stockcardid,
                                                               csoh.processeddate
                         FROM stockmanagement.calculated_stocks_on_hand csoh
                         ORDER BY csoh.stockcardid, csoh.processeddate DESC) csoh_2 ON csoh_2.stockcardid = sc_1.id
             LEFT JOIN referencedata.lots l ON l.id = sc_1.lotid
    GROUP BY sc_1.facilityid, sc_1.programid, sc_1.orderableid
)
   SELECT tsd.facilityid,
          f.name AS facilityname,
          f.code AS facilitycode,
          ft.code AS facilitytype,
          tsd.orderableid,
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
          tsd.totalsoh,
          tsd.totalsoh::numeric * po.priceperpack AS totalprice,
          h.cmm AS realcmm,
          CASE
              WHEN h.cmm IS NULL OR h.cmm <= 0::double precision THEN 0::numeric
              ELSE round(h.cmm::numeric, 2)
              END AS cmm,
          CASE
              WHEN h.cmm IS NULL OR h.cmm <= 0::double precision THEN 0::numeric
              ELSE round(tsd.totalsoh::numeric / round(h.cmm::numeric, 2), 1)
              END AS mos,
          CASE
              WHEN tsd.totalsoh = 0 THEN 'Roptura de stock'::text
              WHEN h.cmm = '-1'::integer::double precision OR h.cmm IS NULL THEN 'Stock regular'::text
              WHEN tsd.totalsoh::double precision < h.cmm THEN 'Eminência de roptura'::text
              WHEN (ft.code = ANY (ARRAY['DPM'::text, 'AI'::text, 'HC'::text])) AND (prnm.programname::text = ANY (ARRAY['TARV'::text, 'MTB'::text, 'Testes Rápidos Diag.'::text])) AND (h.cmm * 4::double precision) < tsd.totalsoh::double precision THEN 'Stock acumulado'::text
              WHEN (ft.code = ANY (ARRAY['DPM'::text, 'AI'::text, 'HC'::text])) AND (prnm.programname::text <> ALL (ARRAY['TARV'::text, 'MTB'::text, 'Testes Rápidos Diag.'::text])) AND (h.cmm * 5::double precision) < tsd.totalsoh::double precision THEN 'Stock acumulado'::text
              WHEN (ft.code <> ALL (ARRAY['DPM'::text, 'AI'::text, 'HC'::text])) AND (prnm.programname::text = ANY (ARRAY['TARV'::text, 'MTB'::text, 'Testes Rápidos Diag.'::text])) AND (h.cmm * 3::double precision) < tsd.totalsoh::double precision THEN 'Stock acumulado'::text
              WHEN (ft.code <> ALL (ARRAY['DPM'::text, 'AI'::text, 'HC'::text])) AND (prnm.programname::text <> ALL (ARRAY['TARV'::text, 'MTB'::text, 'Testes Rápidos Diag.'::text])) AND (h.cmm * 2::double precision) < tsd.totalsoh::double precision THEN 'Stock acumulado'::text
              ELSE 'Stock regular'::text
              END AS stockstatus,
          tsd.expirationdate AS earlistdrugexpriydate,
          tsd.processeddate AS lastupdatetime
   FROM temp_stack_data tsd
            LEFT JOIN siglusintegration.program_report_name_mapping prnm ON prnm.programid = tsd.programid
            LEFT JOIN referencedata.facilities f ON f.id = tsd.facilityid
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
                        WHERE o_1.versionnumber = o_1.latestversion) o ON o.id = tsd.orderableid
            LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id
            LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
            LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid
            LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
            LEFT JOIN ( SELECT DISTINCT ON (hc.facilitycode, hc.productcode) hc.facilitycode,
                                                                             hc.productcode,
                                                                             first_value(hc.cmm) OVER (PARTITION BY hc.facilitycode, hc.productcode ORDER BY hc.periodend DESC) AS cmm
                        FROM siglusintegration.hf_cmms hc) h ON h.facilitycode::text = f.code AND h.productcode::text = o.code::text
            LEFT JOIN ( SELECT program_orderables.id,
                               program_orderables.priceperpack,
                               program_orderables.orderableid,
                               program_orderables.programid,
                               program_orderables.orderableversionnumber
                        FROM referencedata.program_orderables
                        WHERE ((program_orderables.orderableid, program_orderables.orderableversionnumber) IN ( SELECT program_orderables_1.orderableid,
                                                                                                                       max(program_orderables_1.orderableversionnumber) AS max
                                                                                                                FROM referencedata.program_orderables program_orderables_1
                                                                                                                GROUP BY program_orderables_1.orderableid))) po ON po.orderableid = tsd.orderableid;