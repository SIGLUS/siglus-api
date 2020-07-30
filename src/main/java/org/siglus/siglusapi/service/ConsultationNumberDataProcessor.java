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
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.service.mapper.ConsultationNumberLineItemMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationNumberDataProcessor implements UsageReportDataProcessor {

  private final ConsultationNumberLineItemRepository repo;

  private final ConsultationNumberLineItemMapper mapper;

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnableConsultationNumber();
  }

  @Override
  public void doInitiate(SiglusRequisitionDto requisition,
      List<UsageTemplateColumnSection> sectionTemplates) {
    List<ConsultationNumberLineItem> saved = createLineItemsFromTemplate(requisition.getId(),
        sectionTemplates);
    requisition.setConsultationNumberLineItems(mapper.fromLineItems(saved));
  }

  @Override
  public void get(SiglusRequisitionDto requisition) {
    requisition
        .setConsultationNumberLineItems(
            mapper.fromLineItems(repo.findByRequisitionId(requisition.getId())));
  }

  @Override
  public void update(SiglusRequisitionDto requisition,
      SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<ConsultationNumberLineItem> lineItems =
        mapper.fromGroups(requisition.getConsultationNumberLineItems());
    for (ConsultationNumberLineItem lineItem : lineItems) {
      lineItem.setRequisitionId(requisition.getId());
    }

    List<ConsultationNumberLineItem> updated = repo.save(lineItems);
    log.info("update consultation number line items by requisition id: {}", requisition.getId());
    siglusRequisitionUpdatedDto.setConsultationNumberLineItems(mapper.fromLineItems(updated));
  }

  @Override
  public void delete(UUID requisitionId) {
    List<ConsultationNumberLineItem> lineItems = repo.findByRequisitionId(requisitionId);
    log.info("delete consultation number line items by requisition id: {}", requisitionId);
    repo.delete(lineItems);
  }

  private List<ConsultationNumberLineItem> createLineItemsFromTemplate(UUID requisitionId,
      List<UsageTemplateColumnSection> sectionTemplates) {
    List<ConsultationNumberLineItem> lineItems = sectionTemplates.stream()
        .filter(
            sectionTemplate -> sectionTemplate.getCategory() == UsageCategory.CONSULTATIONNUMBER)
        .map(this::toLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    for (ConsultationNumberLineItem lineItem : lineItems) {
      lineItem.setRequisitionId(requisitionId);
    }
    log.info("save consultation number line items by requisition id: {}", requisitionId);
    return repo.save(lineItems);
  }

  private List<ConsultationNumberLineItem> toLineItems(UsageTemplateColumnSection sectionTemplate) {
    return sectionTemplate.getColumns().stream()
        .map(columnTemplate -> {
          ConsultationNumberLineItem lineItem = new ConsultationNumberLineItem();
          lineItem.setGroup(sectionTemplate.getName());
          lineItem.setColumn(columnTemplate.getName());
          return lineItem;
        })
        .collect(Collectors.toList());
  }

}
