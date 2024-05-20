-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_expired_removed_products;
create view dashboard.vw_expired_removed_products as
select
provincename, provincefacilitycode, districtname, districtfacilitycode, facilityname, facilitycode, programname, productcode, productname, lotcode, expirationdate, sum(amountremoved) as amountremoved, removeddate
from
(
	SELECT
		CASE
		    WHEN pgz.name IS NULL THEN gz.name
		    ELSE pgz.name
		END AS provincename,
	vfs.provincefacilitycode,
	gz.name AS districtname,
	vfs.districtfacilitycode,
	f.name AS facilityname,
	f.code AS facilitycode,
	p.name AS programname,
	o.code AS productcode,
	o.fullproductname AS productname,
	l.lotcode,
	l.expirationdate,
	scli.quantity AS amountremoved,
	scli.occurreddate AS removeddate
	FROM stockmanagement.stock_cards sc
	LEFT JOIN referencedata.facilities f ON sc.facilityid = f.id
	LEFT JOIN dashboard.vw_facility_supplier vfs ON f.code = vfs.facilitycode
	LEFT JOIN referencedata.geographic_zones gz ON f.geographiczoneid = gz.id
	LEFT JOIN referencedata.geographic_zones pgz ON gz.parentid = pgz.id
	LEFT JOIN referencedata.programs p ON sc.programid = p.id
	LEFT JOIN
	(
		SELECT DISTINCT ON (orderables.id) orderables.id,
	    orderables.fullproductname,
	    orderables.code,
	    orderables.versionnumber
		FROM referencedata.orderables
	  	ORDER BY orderables.id, orderables.versionnumber DESC
	) o ON sc.orderableid = o.id
	LEFT JOIN referencedata.lots l ON sc.lotid = l.id
	LEFT JOIN stockmanagement.stock_card_line_items scli ON sc.id = scli.stockcardid
	WHERE scli.reasonid = '23f7d0d4-02ce-11ef-9071-12c51f50079a'::uuid
) t
GROUP BY provincename, provincefacilitycode, districtname, districtfacilitycode, facilityname, facilitycode, programname, productcode, productname, lotcode, expirationdate, removeddate;
