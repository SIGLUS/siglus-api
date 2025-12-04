/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.HistoricalDataPersistent;
import org.siglus.siglusapi.repository.dto.FacilityLastRequisitionTimeDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface HistoricalDataRepository extends
    JpaRepository<HistoricalDataPersistent, UUID> {

  @Modifying
  @Transactional
  @Query(
      value = "TRUNCATE TABLE dashboard.historical_data_persistent_data RESTART IDENTITY",
      nativeQuery = true
  )
  void truncateHistoricalData();


  @Modifying
  @Query(value = "refresh MATERIALIZED VIEW dashboard.vw_historical_data;", nativeQuery = true)
  void insertDataFromLastUpdateDate();

  @Query(name = "HistoricalDateService.getFacilityLatestRequisitionDateByFacilitys", nativeQuery = true)
  List<FacilityLastRequisitionTimeDto> getFacilityLatestRequisitionDate(
      @Param("facilityIds") Collection<UUID> facilityIds);

  @Query(name = "HistoricalDateService.getFacilityLatestRequisitionDate", nativeQuery = true)
  List<FacilityLastRequisitionTimeDto> getFacilityLatestRequisitionDate();

  @Modifying
  @Transactional
  @Query(value = "INSERT INTO dashboard.historical_data_persistent_data ("
      + "id, "
      + "periodid, "
      + "startdate, "
      + "enddate, "
      + "periodname, "
      + "orderableid, "
      + "facilityid, "
      + "province, "
      + "district, "
      + "facilitytype, "
      + "facilitycode, "
      + "facilityname, "
      + "productcode, "
      + "productname, "
      + "initialstock, "
      + "consumptions, "
      + "entries, "
      + "adjustments, "
      + "closestock, "
      + "requisitiononperiod, "
      + "cmm, "
      + "mos, "
      + "approvedquantity, "
      + "requestedquantity, "
      + "expiredquantity, "
      + "provincefacilitycode, "
      + "districtfacilitycode, "
      + "periodtype, "
      + "programname"
      + ") "
      + "SELECT DISTINCT ON (pp.id, f.id, o.id) "
      + "uuid_generate_v4() AS id, "
      + "pp.id AS periodid, "
      + "pp.startdate, "
      + "pp.enddate, "
      + "pp.name AS periodname, "
      + "o.id AS orderableid, "
      + "f.id AS facilityid, "
      + "gz_prov.name AS province, "
      + "gz.name AS district, "
      + "ft.code AS facilitytype, "
      + "f.code AS facilitycode, "
      + "f.name AS facilityname, "
      + "o.code AS productcode, "
      + "o.fullproductname AS productname, "
      + "rli.beginningbalance AS initialstock, "
      + "rli.totalconsumedquantity AS consumptions, "
      + "rli.totalreceivedquantity AS entries, "
      + "rli.totallossesandadjustments AS adjustments, "
      + "rli.stockonhand AS closestock, "
      + "CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ', "
      + "CASE TO_CHAR(pp.startdate, 'MM') "
      + "WHEN '01' THEN 'Jan' WHEN '02' THEN 'Fev' WHEN '03' THEN 'Mar' WHEN '04' THEN 'Abr' "
      + "WHEN '05' THEN 'Mai' WHEN '06' THEN 'Jun' WHEN '07' THEN 'Jul' WHEN '08' THEN 'Ago' "
      + "WHEN '09' THEN 'Set' WHEN '10' THEN 'Out' WHEN '11' THEN 'Nov' WHEN '12' THEN 'Dez' "
      + "END, ' ', TO_CHAR(pp.startdate, 'YYYY'), ' - ', TO_CHAR(pp.enddate, 'DD'), ' ', "
      + "CASE TO_CHAR(pp.enddate, 'MM') "
      + "WHEN '01' THEN 'Jan' WHEN '02' THEN 'Fev' WHEN '03' THEN 'Mar' WHEN '04' THEN 'Abr' "
      + "WHEN '05' THEN 'Mai' WHEN '06' THEN 'Jun' WHEN '07' THEN 'Jul' WHEN '08' THEN 'Ago' "
      + "WHEN '09' THEN 'Set' WHEN '10' THEN 'Out' WHEN '11' THEN 'Nov' WHEN '12' THEN 'Dez' "
      + "END, ' ', TO_CHAR(pp.enddate, 'YYYY')) AS requisitiononperiod, "
      + "CASE WHEN hc.cmm IS NULL OR hc.cmm <= 0 THEN 0 ELSE ROUND(CAST(hc.cmm AS numeric), 2) END AS cmm, "
      + "CASE WHEN hc.cmm IS NULL OR hc.cmm <= 0 THEN 0 "
      + "ELSE ROUND(CAST(rli.stockonhand AS numeric) / ROUND(CAST(hc.cmm AS numeric), 2), 1) END AS mos, "
      + "CASE WHEN appandreq.approvedquantity IS NULL OR p.code <> 'VC' THEN 0 "
      + "ELSE appandreq.approvedquantity END AS approvedquantity, "
      + "CASE WHEN appandreq.requestedquantity IS NULL OR p.code <> 'VC' THEN 0 "
      + "ELSE appandreq.requestedquantity END AS requestedquantity, "
      + "expired.expiredquantity, "
      + "vfs.provincefacilitycode, "
      + "vfs.districtfacilitycode, "
      + "CASE WHEN ps.code LIKE 'M%' THEN 'Mensal' WHEN ps.code LIKE 'Q%' THEN 'Trimestral' END AS periodtype, "
      + "p.name AS programname "
      + "FROM requisition.requisition_line_items rli "
      + "LEFT JOIN requisition.requisitions r ON rli.requisitionid = r.id "
      + "LEFT JOIN referencedata.programs p ON p.id = r.programid "
      + "LEFT JOIN ("
      + "    SELECT * FROM ("
      + " SELECT id, code, fullproductname, versionnumber, MAX(versionnumber) OVER(PARTITION BY id) "
      + " AS latestversion "
      + " FROM referencedata.orderables "
      + "    ) o WHERE o.versionnumber = o.latestversion"
      + ") o ON rli.orderableid = o.id "
      + "LEFT JOIN referencedata.facilities f ON f.id = r.facilityid "
      + "LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id "
      + "LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid "
      + "LEFT JOIN referencedata.geographic_zones gz_prov ON gz_prov.id = gz.parentid "
      + "LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code "
      + "LEFT JOIN referencedata.processing_periods pp ON pp.id = r.processingperiodid "
      + "LEFT JOIN siglusintegration.hf_cmms hc ON hc.facilitycode = f.code AND hc.productcode = o.code "
      + "AND hc.periodbegin = pp.startdate AND hc.periodend = pp.enddate "
      + "LEFT JOIN ("
      + "    SELECT e.facilityid, e.periodid, e.orderableid, SUM(e.expiredquantity) AS expiredquantity "
      + "    FROM ("
      + "        SELECT r.facilityid, rli.orderableid, pp.id AS periodid, sc.lotid, "
      + "csoh.stockonhand AS expiredquantity, "
      + "               ROW_NUMBER() "
      + "OVER(PARTITION BY r.facilityid, rli.orderableid, pp.id, sc.lotid ORDER BY csoh.occurreddate DESC)"
      + " AS row_number "
      + "        FROM stockmanagement.stock_cards sc "
      + "        JOIN requisition.requisition_line_items rli ON rli.orderableid = sc.orderableid "
      + "        JOIN requisition.requisitions r ON r.id = rli.requisitionid AND r.facilityid = sc.facilityid "
      + "        LEFT JOIN referencedata.processing_periods pp ON r.processingperiodid = pp.id "
      + "        LEFT JOIN stockmanagement.calculated_stocks_on_hand csoh ON csoh.stockcardid = sc.id "
      + "AND csoh.occurreddate <= pp.enddate AND csoh.occurreddate >= pp.startdate "
      + "        LEFT JOIN referencedata.lots l ON l.id = sc.lotid AND l.expirationdate <= pp.enddate"
      + " AND l.expirationdate >= pp.startdate "
      + "        WHERE l.id IS NOT NULL AND r.emergency = false "
      + "    ) e "
      + "    WHERE row_number = 1 "
      + "    GROUP BY e.facilityid, e.periodid, e.orderableid "
      + ") expired ON r.facilityid = expired.facilityid AND rli.orderableid = expired.orderableid "
      + "AND r.processingperiodid = expired.periodid "
      + "LEFT JOIN referencedata.processing_schedules ps ON pp.processingscheduleid = ps.id "
      + "LEFT JOIN ("
      + "    SELECT r.processingperiodid, r.facilityid, rli.orderableid, SUM(rli.approvedquantity) "
      + "AS approvedquantity, SUM(rli.requestedquantity) AS requestedquantity "
      + "    FROM requisition.requisition_line_items rli "
      + "    JOIN requisition.requisitions r ON r.id = rli.requisitionid "
      + "    GROUP BY r.processingperiodid, r.facilityid, rli.orderableid "
      + ") appandreq ON appandreq.processingperiodid = pp.id AND appandreq.facilityid = r.facilityid "
      + "AND appandreq.orderableid = rli.orderableid "
      + "WHERE r.status IN ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') "
      + "AND r.emergency = false "
      + "AND r.facilityid = :facilityId "
      + "AND pp.startdate >= :startDate "
      + "AND pp.enddate <= :endDate "
      + "ON CONFLICT (periodid, facilityid, orderableid) DO UPDATE "
      + "SET id = excluded.id, "
      + "periodid = excluded.periodid, "
      + "startdate = excluded.startdate, "
      + "enddate = excluded.enddate, "
      + "periodname = excluded.periodname, "
      + "orderableid = excluded.orderableid, "
      + "facilityid = excluded.facilityid, "
      + "province = excluded.province, "
      + "district = excluded.district, "
      + "facilitytype = excluded.facilitytype, "
      + "facilitycode = excluded.facilitycode, "
      + "facilityname = excluded.facilityname, "
      + "productcode = excluded.productcode, "
      + "productname = excluded.productname, "
      + "initialstock = excluded.initialstock, "
      + "consumptions = excluded.consumptions, "
      + "entries = excluded.entries, "
      + "adjustments = excluded.adjustments, "
      + "closestock = excluded.closestock, "
      + "requisitiononperiod = excluded.requisitiononperiod, "
      + "cmm = excluded.cmm, "
      + "mos = excluded.mos, "
      + "approvedquantity = excluded.approvedquantity, "
      + "requestedquantity = excluded.requestedquantity, "
      + "expiredquantity = excluded.expiredquantity, "
      + "provincefacilitycode = excluded.provincefacilitycode, "
      + "districtfacilitycode = excluded.districtfacilitycode, "
      + "periodtype = excluded.periodtype, "
      + "programname = excluded.programname", nativeQuery = true)
  void updateFacilityHistoricalData(@Param("facilityId") UUID facilityId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);


}
