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

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.TracerDrugPersistentData;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExcelDto;
import org.siglus.siglusapi.repository.dto.ProductLotSohDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
public interface TracerDrugRepository
    extends JpaRepository<TracerDrugPersistentData, UUID>, TracerDrugRepositoryCustom {

  @Query(name = "RequisitionGeographicZone.findAllZones", nativeQuery = true)
  List<RequisitionGeographicZonesDto> getAllRequisitionGeographicZones();

  @Query(name = "TracerDrug.findTracerDrug", nativeQuery = true)
  List<TracerDrugDto> getTracerDrugInfo();

  @Query(name = "TracerDrug.findExcelDetail", nativeQuery = true)
  List<TracerDrugExcelDto> getTracerDrugExcelInfo(
      String startDate, String endDate, String productCode, List<String> facilityCodes);

  @Query(
      value =
          "select cmm.* from siglusintegration.hf_cmms cmm "
              + "    where cmm.facilitycode in :facilityCodes "
              + "        and cmm.productcode in "
              + "            (select code from referencedata.orderables "
              + "                where cast(orderables.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)) "
              + "        and cmm.periodend between :beginDate and :endDate",
      nativeQuery = true)
  List<HfCmm> getTracerDrugCmm(
      @Param("beginDate") String beginDate,
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(
      value =
          "select fa.code as facilitycode, od.code as productcode, sc.lotid, soh.stockonhand, soh.occurreddate from "
              + "stockmanagement.calculated_stocks_on_hand soh join "
              + "    stockmanagement.stock_cards sc on sc.id = soh.stockcardid join "
              + "    referencedata.facilities fa on sc.facilityid=fa.id join "
              + "    referencedata.orderables od on od.id=sc.orderableid "
              + "    where fa.code in :facilityCodes "
              + "        and sc.orderableid in "
              + "            (select id from referencedata.orderables "
              + "                where cast(orderables.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)) "
              + "        and soh.occurreddate between :beginDate and :endDate",
      nativeQuery = true)
  List<ProductLotSohDto> getTracerDrugSoh(
      @Param("beginDate") String beginDate,
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(
      value =
          "select * from (select\n"
              + "    fa.code as facilitycode, od.code as productcode, sc.lotid, stockonhand, occurreddate,\n"
              + "    row_number() over (partition by stockcardid order by occurreddate desc) from "
              + "    stockmanagement.calculated_stocks_on_hand csoh\n"
              + "    join stockmanagement.stock_cards sc on csoh.stockcardid = sc.id\n"
              + "    join referencedata.orderables od on od.id=sc.orderableid\n"
              + "    join referencedata.facilities fa on fa.id=sc.facilityid\n"
              + "    where fa.code in :facilityCodes and "
              + "       cast(od.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb) and "
              + "       csoh.occurreddate < :endDate)\n"
              + "    soh where soh.row_number=1;",
      nativeQuery = true)
  List<ProductLotSohDto> getLastTracerDrugSohTillDate(
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(
      value =
          "select * from (select cmm.facilitycode, cmm.productcode, cmm.periodend, cmm.cmm,\n"
              + " row_number() over (partition by cmm.facilitycode, cmm.productcode order by cmm.periodend desc)"
              + "    from siglusintegration.hf_cmms cmm\n"
              + "    join referencedata.orderables od on od.code=cmm.productcode\n"
              + "    where cmm.facilitycode in :facilityCodes\n"
              + "        and cast(od.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)\n"
              + "        and cmm.periodend  < :endDate) sub where sub.row_number=1",
      nativeQuery = true)
  List<HfCmm> getLastTracerDrugCmmTillDate(
      @Param("endDate") String endDate, @Param("facilityCodes") List<String> facilityCodes);
}
