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

public interface HistoricalDataRepository extends
    JpaRepository<HistoricalDataPersistent, UUID> {

  @Modifying
  @Query(value = "refresh MATERIALIZED VIEW dashboard.vw_historical_data;", nativeQuery = true)
  void insertDataFromLastUpdateDate();

  @Query(name = "HistoricalDateService.getFacilityLatestRequisitionDateByFacilitys", nativeQuery = true)
  List<FacilityLastRequisitionTimeDto> getFacilityLatestRequisitionDate(
      @Param("facilityIds") Collection<UUID> facilityIds);

  @Query(name = "HistoricalDateService.getFacilityLatestRequisitionDate", nativeQuery = true)
  List<FacilityLastRequisitionTimeDto> getFacilityLatestRequisitionDate();

  @Modifying
  @Query(value = "insert\n"
      + "  into\n"
      + "  dashboard.historical_data_persistent_data (id,\n"
      + "  periodid,\n"
      + "  startdate,\n"
      + "  enddate,\n"
      + "  orderableid,\n"
      + "  facilityid,\n"
      + "  province,\n"
      + "  district,\n"
      + "  facilitytype,\n"
      + "  facilitycode,\n"
      + "  facilityname,\n"
      + "  productcode,\n"
      + "  productname,\n"
      + "  initialstock,\n"
      + "  consumptions,\n"
      + "  entries,\n"
      + "  adjustments,\n"
      + "  closestock,\n"
      + "  requisitiononperiod,\n"
      + "  cmm,\n"
      + "  mos,\n"
      + "  approvedquantity,\n"
      + "  requestedquantity,\n"
      + "  expiredquantity ,\n"
      + "  provincefacilitycode,\n"
      + "  districtfacilitycode,\n"
      + "  periodtype) \n"
      + "       select\n"
      + "  uuid_generate_v4() as id,\n"
      + "  pp.id as periodid,\n"
      + "  pp.startdate,\n"
      + "  pp.enddate,\n"
      + "  o.id as orderableid,\n"
      + "  f.id as facilityid,\n"
      + "  gz_prov.name as province,\n"
      + "  gz.name as district,\n"
      + "  ft.code as facilitytype,\n"
      + "  f.code as facilitycode,\n"
      + "  f.name as facilityname,\n"
      + "  o.code as productcode,\n"
      + "  o.fullproductname as productname,\n"
      + "  rli.beginningbalance as initialstock,\n"
      + "  rli.adjustedconsumption as consumptions,\n"
      + "  rli.totalreceivedquantity as entries,\n"
      + "  rli.totallossesandadjustments as adjustments,\n"
      + "  rli.stockonhand as closestock,\n"
      + "  CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ',\n"
      + "                     (case TO_CHAR(pp.startdate, 'MM')\n"
      + "                          when '01' then 'Jan'\n"
      + "                          when '02' then 'Fev'\n"
      + "                          when '03' then 'Mar'\n"
      + "                          when '04' then 'Abr'\n"
      + "                          when '05' then 'Mai'\n"
      + "                          when '06' then 'Jun'\n"
      + "                          when '07' then 'Jul'\n"
      + "                          when '08' then 'Ago'\n"
      + "                          when '09' then 'Set'\n"
      + "                          when '10' then 'Out'\n"
      + "                          when '11' then 'Nov'\n"
      + "                          when '12' then 'Dez' end), ' ',\n"
      + "                     TO_CHAR(pp.startdate, 'YYYY'), ' - ', TO_CHAR(pp.enddate, 'DD'), ' ',\n"
      + "                     (case TO_CHAR(pp.enddate, 'MM')\n"
      + "                          when '01' then 'Jan'\n"
      + "                          when '02' then 'Fev'\n"
      + "                          when '03' then 'Mar'\n"
      + "                          when '04' then 'Abr'\n"
      + "                          when '05' then 'Mai'\n"
      + "                          when '06' then 'Jun'\n"
      + "                          when '07' then 'Jul'\n"
      + "                          when '08' then 'Ago'\n"
      + "                          when '09' then 'Set'\n"
      + "                          when '10' then 'Out'\n"
      + "                          when '11' then 'Nov'\n"
      + "                          when '12' then 'Dez' end), ' ', TO_CHAR(pp.enddate, 'YYYY')) requisitiononperiod,\n"
      + "  case \n"
      + "    when hc.cmm is null\n"
      + "    or hc.cmm <= cast(0 as double precision) then cast(0 as numeric)\n"
      + "    else round(cast(hc.cmm as numeric), 2)\n"
      + "  end as cmm,\n"
      + "  case\n"
      + "    when hc.cmm is null\n"
      + "    or hc.cmm <= cast(0 as double precision) then cast(0 as numeric)\n"
      + "    else round(cast(rli.stockonhand as numeric) / round(cast(hc.cmm as numeric), 2), 1)\n"
      + "  end as mos,\n"
      + "  case \n"
      + "    when appandreq.approvedquantity is null\n"
      + "    or p.code <> 'VC' then 0\n"
      + "    else appandreq.approvedquantity\n"
      + "  end as approvedquantity,\n"
      + "  case\n"
      + "    when appandreq.requestedquantity is null\n"
      + "    or p.code <> 'VC' then 0\n"
      + "    else appandreq.requestedquantity\n"
      + "  end as requestedquantity,\n"
      + "  expired.expiredquantity,\n"
      + "  vfs.provincefacilitycode,\n"
      + "  vfs.districtfacilitycode,\n"
      + "  case\n"
      + "    when ps.code = 'M1' then 'monthly'\n"
      + "    when ps.code = 'Q1' then 'quarterly'\n"
      + "  end as periodtype\n"
      + "from\n"
      + "  requisition.requisition_line_items rli\n"
      + "left join requisition.requisitions r on\n"
      + "  rli.requisitionid = r.id\n"
      + "left join referencedata.programs p on\n"
      + "  p.id = r.programid\n"
      + "left join (\n"
      + "  select\n"
      + "    *\n"
      + "  from\n"
      + "    (\n"
      + "    select\n"
      + "      id,\n"
      + "      code,\n"
      + "      fullproductname,\n"
      + "      versionnumber,\n"
      + "      max(versionnumber) over(partition by id) latestversion\n"
      + "    from\n"
      + "      referencedata.orderables) o\n"
      + "  where\n"
      + "    o.versionnumber = o.latestversion) o\n"
      + "                          on\n"
      + "  rli.orderableid = o.id\n"
      + "left join referencedata.facilities f on\n"
      + "  f.id = r.facilityid\n"
      + "left join referencedata.facility_types ft on\n"
      + "  f.typeid = ft.id\n"
      + "left join referencedata.geographic_zones gz on\n"
      + "  gz.id = f.geographiczoneid\n"
      + "left join referencedata.geographic_zones gz_prov on\n"
      + "  gz_prov.id = gz.parentid\n"
      + "left join dashboard.vw_facility_supplier vfs on\n"
      + "  vfs.facilitycode = f.code\n"
      + "left join referencedata.processing_periods pp on\n"
      + "  pp.id = r.processingperiodid\n"
      + "left join\n"
      + "            siglusintegration.hf_cmms hc on\n"
      + "  hc.facilitycode = f.code\n"
      + "  and hc.productcode = o.code\n"
      + "  and hc.periodbegin = pp.startdate\n"
      + "  and hc.periodend = pp.enddate\n"
      + "left join (\n"
      + "  select\n"
      + "    e.facilityid,\n"
      + "    e.periodid,\n"
      + "    e.orderableid,\n"
      + "    sum(e.expiredquantity) as expiredquantity\n"
      + "  from\n"
      + "    (\n"
      + "    select\n"
      + "      r.facilityid,\n"
      + "      rli.orderableid,\n"
      + "      pp.id as periodid,\n"
      + "      sc.lotid,\n"
      + "      csoh.stockonhand as expiredquantity,\n"
      + "      row_number() over(partition by r.facilityid,\n"
      + "      rli.orderableid ,\n"
      + "      pp.id,\n"
      + "      sc.lotid\n"
      + "    order by\n"
      + "      csoh.occurreddate desc)\n"
      + "    from\n"
      + "      stockmanagement.stock_cards sc\n"
      + "    join requisition.requisition_line_items rli on\n"
      + "      rli.orderableid = sc.orderableid\n"
      + "    join requisition.requisitions r on\n"
      + "      r.id = rli.requisitionid\n"
      + "      and r.facilityid = sc.facilityid\n"
      + "    left join referencedata.processing_periods pp on\n"
      + "      r.processingperiodid = pp.id\n"
      + "    left join stockmanagement.calculated_stocks_on_hand csoh on\n"
      + "      csoh.stockcardid = sc.id\n"
      + "      and csoh.occurreddate <= pp.enddate\n"
      + "      and csoh.occurreddate >= pp.startdate\n"
      + "    left join referencedata.lots l on\n"
      + "      l.id = sc.lotid\n"
      + "      and l.expirationdate <= pp.enddate\n"
      + "      and l.expirationdate >= pp.startdate\n"
      + "    where\n"
      + "      l.id is not null\n"
      + "      and r.emergency = false) e\n"
      + "  where\n"
      + "    row_number = 1\n"
      + "  group by\n"
      + "    e.facilityid,\n"
      + "    e.periodid,\n"
      + "    e.orderableid) expired on\n"
      + "  r.facilityid = expired.facilityid\n"
      + "  and rli.orderableid = expired.orderableid\n"
      + "  and r.processingperiodid = expired.periodid\n"
      + "left join referencedata.processing_schedules ps on\n"
      + "  pp.processingscheduleid = ps.id\n"
      + "left join (\n"
      + "  select\n"
      + "    r.processingperiodid,\n"
      + "    r.facilityid,\n"
      + "    rli.orderableid,\n"
      + "    sum(rli.approvedquantity) as approvedquantity,\n"
      + "    sum(rli.requestedquantity) as requestedquantity\n"
      + "  from\n"
      + "    requisition.requisition_line_items rli\n"
      + "  join requisition.requisitions r on\n"
      + "    r.id = rli.requisitionid\n"
      + "  group by\n"
      + "    r.processingperiodid,\n"
      + "    r.facilityid,\n"
      + "    rli.orderableid) appandreq\n"
      + "                          on\n"
      + "  appandreq.processingperiodid = pp.id\n"
      + "  and appandreq.facilityid = r.facilityid\n"
      + "  and\n"
      + "                             appandreq.orderableid = rli.orderableid\n"
      + "where\n"
      + "  ps.code in ('M1', 'Q1')\n"
      + "  and r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER')\n"
      + "  and r.emergency = false\n"
      + "  and r.facilityid = :facilityId\n"
      + "  and pp.startdate > :startDate\n"
      + "  and pp.enddate < :endDate\n"
      + "on conflict (periodid, facilityid, orderableid) do nothing", nativeQuery = true)
  void updateFacilityHistoricalData(@Param("facilityId") UUID facilityId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
