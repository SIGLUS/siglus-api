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
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientDataProcessor implements UsageReportDataProcessor {

  private final PatientLineItemRepository repo;

  private final PatientLineItemMapper mapper;

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnablePatientLineItem();
  }

  @Override
  public void doInitiate(SiglusRequisitionDto requisition,
      List<UsageTemplateColumnSection> sectionTemplates) {
    List<PatientLineItem> saved = createLineItemsFromTemplate(requisition.getId(),
        sectionTemplates);
    requisition.setPatientLineItems(mapper.from(saved));
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
    log.info("save patient line items by requisition id: {}", requisitionId);
    return repo.save(lineItems);
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
