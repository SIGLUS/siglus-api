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
import org.siglus.siglusapi.domain.TracerDrugPersistentData;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExcelDto;
import org.siglus.siglusapi.repository.dto.ProductCmm;
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

  @Query(name = "TracerDrug.getTracerDrugCmm", nativeQuery = true)
  List<ProductCmm> getTracerDrugCmm(
      @Param("beginDate") String beginDate,
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(name = "TracerDrugReport.getTracerDrugSoh", nativeQuery = true)
  List<ProductLotSohDto> getTracerDrugSoh(
      @Param("beginDate") String beginDate,
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(name = "TracerDrugReport.getLastTracerDrugSohTillDate", nativeQuery = true)
  List<ProductLotSohDto> getLastTracerDrugSohTillDate(
      @Param("endDate") String endDate,
      @Param("facilityCodes") List<String> facilityCodes);

  @Query(name = "TracerDrug.getLastTracerDrugCmmTillDate", nativeQuery = true)
  List<ProductCmm> getLastTracerDrugCmmTillDate(
      @Param("endDate") String endDate, @Param("facilityCodes") List<String> facilityCodes);
}
