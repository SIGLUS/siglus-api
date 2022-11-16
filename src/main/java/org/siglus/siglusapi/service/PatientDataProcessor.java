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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientDataProcessor implements UsageReportDataProcessor {

  private final SiglusUsageReportService siglusUsageReportService;

  private final PatientLineItemRepository repo;

  private final PatientLineItemMapper mapper;

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnablePatientLineItem();
  }

  @Override
  public void doInitiate(SiglusRequisitionDto requisition,
      List<UsageTemplateColumnSection> sectionTemplates) {
    List<PatientLineItem> patientLineItems = createLineItemsFromTemplate(requisition.getId(), sectionTemplates);
    calculateValueForTopLevelFacility(patientLineItems, requisition.getFacilityId(),
        requisition.getProcessingPeriodId(), requisition.getProgramId());
    log.info("save patient line items by requisition id: {}", requisition.getId());
    List<PatientLineItem> save = repo.save(patientLineItems);
    requisition.setPatientLineItems(mapper.from(save));
  }

  private void calculateValueForTopLevelFacility(
      List<PatientLineItem> patientLineItems, UUID facilityId, UUID periodId, UUID programId) {
    if (patientLineItems.isEmpty() || siglusUsageReportService.isNonTopLevelOrNotUsageReports(programId, facilityId)) {
      return;
    }
    Map<String, Integer> itemToSumValue = itemToValue(
        repo.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId));
    Map<String, Integer> itemToMaxValueInLastPeriods = itemToValue(
        repo.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId));
    patientLineItems.forEach(lineItem -> setSumValue(lineItem, itemToSumValue, itemToMaxValueInLastPeriods));
  }

  private Map<String, Integer> itemToValue(List<PatientColumnDto> patientColumnDtos) {
    return patientColumnDtos.stream()
        .collect(Collectors.toMap(PatientColumnDto::getMappingKey,
            dto -> dto.getValue() == null ? 0 : dto.getValue()));
  }

  private void setSumValue(PatientLineItem lineItem,
      Map<String, Integer> itemToSumValue, Map<String, Integer> itemToMaxValueInLastPeriods) {
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
  public void get(SiglusRequisitionDto requisition) {
    requisition
        .setPatientLineItems(mapper.from(repo.findByRequisitionId(requisition.getId())));
  }

  @Override
  public void update(SiglusRequisitionDto requisition,
      SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<PatientLineItem> lineItems = requisition.getPatientLineItems().stream()
        .map(mapper::from)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    for (PatientLineItem lineItem : lineItems) {
      lineItem.setRequisitionId(requisition.getId());
    }

    List<PatientLineItem> updated = repo.save(lineItems);
    log.info("update patient line items by requisition id: {}", requisition.getId());
    siglusRequisitionUpdatedDto.setPatientLineItems(mapper.from(updated));
  }

  @Override
  public void delete(UUID requisitionId) {
    List<PatientLineItem> lineItems = repo.findByRequisitionId(requisitionId);
    log.info("delete patient line items by requisition id: {}", requisitionId);
    repo.delete(lineItems);
  }

  private List<PatientLineItem> createLineItemsFromTemplate(UUID requisitionId,
      List<UsageTemplateColumnSection> sectionTemplates) {
    List<PatientLineItem> lineItems = sectionTemplates.stream()
        .filter(sectionTemplate -> sectionTemplate.getCategory() == UsageCategory.PATIENT)
        .map(this::toLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    for (PatientLineItem lineItem : lineItems) {
      lineItem.setRequisitionId(requisitionId);
    }
    return lineItems;
  }

  private List<PatientLineItem> toLineItems(UsageTemplateColumnSection sectionTemplate) {
    return sectionTemplate.getColumns().stream()
        .filter(UsageTemplateColumn::getIsDisplayed)
        .map(columnTemplate -> {
          PatientLineItem lineItem = new PatientLineItem();
          lineItem.setGroup(sectionTemplate.getName());
          lineItem.setColumn(columnTemplate.getName());
          lineItem.setValue(null);
          return lineItem;
        })
        .collect(Collectors.toList());
  }

}
