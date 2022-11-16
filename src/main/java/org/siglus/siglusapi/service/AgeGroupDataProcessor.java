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

package org.siglus.siglusapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.AgeGroupLineItemDto;
import org.siglus.siglusapi.dto.AgeGroupServiceDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgeGroupDataProcessor implements UsageReportDataProcessor {

  private static final String SERVICE = "service";
  private static final String GROUP = "group";

  private final AgeGroupLineItemRepository ageGroupLineItemRepository;
  private final SiglusUsageReportService siglusUsageReportService;

  @Override
  public void doInitiate(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    List<AgeGroupLineItem> ageGroupLineItems =
        createAgeGroupLineItems(siglusRequisitionDto, templateColumnSections);
    calculateValueForTopLevelFacility(ageGroupLineItems, siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId(), siglusRequisitionDto.getProgramId());
    log.info("save age group line items by requisition id: {}", siglusRequisitionDto.getId());
    List<AgeGroupLineItem> saved = ageGroupLineItemRepository.save(ageGroupLineItems);
    List<AgeGroupServiceDto> serviceDtos = AgeGroupServiceDto.from(saved);
    siglusRequisitionDto.setAgeGroupLineItems(serviceDtos);
  }

  private void calculateValueForTopLevelFacility(
      List<AgeGroupLineItem> ageGroupLineItems, UUID facilityId, UUID periodId, UUID programId) {
    if (ageGroupLineItems.isEmpty() || siglusUsageReportService.isNonTopLevelOrNotUsageReports(programId, facilityId)) {
      return;
    }
    Map<String, Integer> itemToSumValue = itemToValue(
        ageGroupLineItemRepository.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId));
    Map<String, Integer> itemToMaxValueInLastPeriods = itemToValue(
        ageGroupLineItemRepository.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId));
    ageGroupLineItems.forEach(lineItem -> setSumValue(lineItem, itemToSumValue, itemToMaxValueInLastPeriods));
  }

  private Map<String, Integer> itemToValue(List<AgeGroupLineItemDto> ageGroupLineItemDtos) {
    return ageGroupLineItemDtos.stream()
        .collect(
            Collectors.toMap(AgeGroupLineItemDto::getMappingKey, dto -> dto.getValue() == null ? 0 : dto.getValue()));
  }

  private void setSumValue(AgeGroupLineItem lineItem, Map<String, Integer> itemToSumValue,
      Map<String, Integer> itemToMaxValueInLastPeriods) {
    Integer sumValue = itemToSumValue.get(lineItem.getMappingKey());
    Integer maxValueInLastPeriods = itemToMaxValueInLastPeriods.get(lineItem.getMappingKey());
    if (sumValue == null) {
      sumValue = 0;
    }
    if (maxValueInLastPeriods == null) {
      maxValueInLastPeriods = 0;
    }
    lineItem.setValue(sumValue + maxValueInLastPeriods);
  }

  @Override
  public void get(SiglusRequisitionDto siglusRequisitionDto) {
    List<AgeGroupLineItem> ageGroupLineItems = ageGroupLineItemRepository.findByRequisitionId(
        siglusRequisitionDto.getId());
    siglusRequisitionDto.setAgeGroupLineItems(AgeGroupServiceDto.from(ageGroupLineItems));
  }

  @Override
  public void update(SiglusRequisitionDto siglusRequisitionDto, SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<AgeGroupServiceDto> ageGroupLineDtoList = siglusRequisitionDto.getAgeGroupLineItems();
    List<AgeGroupLineItem> save = ageGroupLineItemRepository.save(AgeGroupLineItem.from(ageGroupLineDtoList,
        siglusRequisitionDto.getId()));
    siglusRequisitionUpdatedDto.setAgeGroupLineItems(AgeGroupServiceDto.from(save));
  }

  @Override
  public void delete(UUID requisitionId) {
    ageGroupLineItemRepository.deleteByRequisitionId(requisitionId);
  }

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnableAgeGroup();
  }

  private List<AgeGroupLineItem> createAgeGroupLineItems(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    UsageTemplateColumnSection service = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.AGEGROUP, SERVICE);
    UsageTemplateColumnSection group = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.AGEGROUP, GROUP);
    List<AgeGroupLineItem> ageGroupLineItems = new ArrayList<>();

    for (UsageTemplateColumn templateServiceColumn : service.getColumns()) {
      if (!Boolean.TRUE.equals(templateServiceColumn.getIsDisplayed())) {
        continue;
      }

      for (UsageTemplateColumn templateGroupColumn : group.getColumns()) {
        if (!Boolean.TRUE.equals(templateGroupColumn.getIsDisplayed())) {
          continue;
        }
        ageGroupLineItems.add(AgeGroupLineItem.builder()
            .requisitionId(siglusRequisitionDto.getId())
            .group(templateGroupColumn.getName())
            .service(templateServiceColumn.getName())
            .build());
      }
    }
    return ageGroupLineItems;
  }
}
