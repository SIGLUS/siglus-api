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

package org.siglus.siglusapi.service.task.report;

import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.siglus.siglusapi.dto.AssociatedGeographicZoneDto;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExportDto;
import org.siglus.siglusapi.repository.TracerDrugRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TracerDrugReportService {

  private final TracerDrugRepository tracerDrugRepository;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Value("${tracer.drug.initialize.date}")
  private String tracerDrugInitializeDate;

  @Async
  @Transactional
  public void refreshTracerDrugPersistentData(String startDate, String endDate) {
    log.info("tracer drug persistentData refresh. start = " + System.currentTimeMillis());
    tracerDrugRepository.insertDataWithinSpecifiedTime(startDate, endDate);
    log.info("tracer drug persistentData  refresh. end = " + System.currentTimeMillis());
  }


  @Async
  @Transactional
  public void initializeTracerDrugPersistentData() {
    log.info("tracer drug persistentData initialize. start = " + System.currentTimeMillis());
    refreshTracerDrugPersistentData(tracerDrugInitializeDate, LocalDate.now().toString());
    log.info("tracer drug persistentData initialize. end = " + System.currentTimeMillis());
  }


  public TracerDrugExportDto getTracerDrugExportDto() {
    String facilityCode = siglusFacilityReferenceDataService
        .findOne(authenticationHelper.getCurrentUser().getHomeFacilityId()).getCode();

    List<AssociatedGeographicZoneDto> associatedGeographicZones = getRequisitionGeographicZonesDtos(facilityCode);

    List<TracerDrugDto> tracerDrugInfo = tracerDrugRepository.getTracerDrugInfo();

    return TracerDrugExportDto
        .builder()
        .tracerDrugs(tracerDrugInfo)
        .geographicZones(associatedGeographicZones)
        .build();
  }

  private List<AssociatedGeographicZoneDto> getRequisitionGeographicZonesDtos(String facilityCode) {
    String level = authenticationHelper.getFacilityGeographicZoneLevel();
    List<RequisitionGeographicZonesDto> requisitionGeographicZones = tracerDrugRepository
        .getAllRequisitionGeographicZones();
    if (authenticationHelper.isTheCurrentUserAdmin()) {
      return getDistinctGeographicZones(getAssociatedGeographicZoneDto(requisitionGeographicZones));
    } else if (Objects.equals(level, DISTRICT)) {
      requisitionGeographicZones = getAuthorizedGeographicZonesByDistrictLevel(facilityCode,
          requisitionGeographicZones);
    } else if (Objects.equals(level, PROVINCE)) {
      requisitionGeographicZones = getAuthorizedGeographicZonesByProvinceLevel(facilityCode,
          requisitionGeographicZones);
    } else {
      requisitionGeographicZones = getAuthorizedGeographicZonesBySiteLevel(facilityCode,
          requisitionGeographicZones);
    }
    return getDistinctGeographicZones(getAssociatedGeographicZoneDto(requisitionGeographicZones));
  }

  private List<AssociatedGeographicZoneDto> getAssociatedGeographicZoneDto(
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {

    List<AssociatedGeographicZoneDto> districtGeographicZoneDto = requisitionGeographicZones
        .stream()
        .map(o -> o.getDistrictGeographicZoneDtoFrom(o))
        .collect(Collectors.toList());

    List<AssociatedGeographicZoneDto> provinceGeographicZoneDto = requisitionGeographicZones
        .stream()
        .map(o -> o.getProvinceGeographicZoneDtoFrom(o))
        .collect(Collectors.toList());
    return ListUtils.union(districtGeographicZoneDto, provinceGeographicZoneDto);
  }


  private List<AssociatedGeographicZoneDto> getDistinctGeographicZones(
      List<AssociatedGeographicZoneDto> requisitionDistrictGeographicZones) {
    return requisitionDistrictGeographicZones.stream().collect(
        Collectors.collectingAndThen(
            Collectors.toCollection(() ->
                new TreeSet<>(Comparator.comparing(AssociatedGeographicZoneDto::getCode))), ArrayList::new));
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesByDistrictLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getDistrictFacilityCode() != null)
        .filter(o -> o.getDistrictFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesByProvinceLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getProvinceFacilityCode() != null)
        .filter(o -> o.getProvinceFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesBySiteLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getFacilityCode() != null)
        .filter(o -> o.getFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }

}
