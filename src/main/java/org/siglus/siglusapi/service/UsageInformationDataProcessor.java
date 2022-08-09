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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.dto.BaseDto;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UsageInformationDataProcessor implements UsageReportDataProcessor {

  private static final String INFORMATION = "information";

  private static final String SERVICE = "service";

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private UsageInformationLineItemRepository usageInformationLineItemRepository;

  @Autowired
  private OrderableKitRepository orderableKitRepository;

  @Override
  public void doInitiate(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    List<UsageInformationLineItem> lineItems = createUsageInformationLineItems(
        siglusRequisitionDto, templateColumnSections);
    log.info("save usage information line items by requisition id: {}",
        siglusRequisitionDto.getId());
    List<UsageInformationLineItem> lineItemsSaved = usageInformationLineItemRepository
        .save(lineItems);
    List<UsageInformationServiceDto> serviceDtos = UsageInformationServiceDto.from(lineItemsSaved);
    siglusRequisitionDto.setUsageInformationLineItems(serviceDtos);
  }

  @Override
  public void get(SiglusRequisitionDto siglusRequisitionDto) {
    log.info("find usage information line items by requisition id: {}",
        siglusRequisitionDto.getId());
    List<UsageInformationLineItem> lineItems = usageInformationLineItemRepository
        .findByRequisitionId(siglusRequisitionDto.getId());
    List<UsageInformationServiceDto> serviceDtos = UsageInformationServiceDto.from(lineItems);
    siglusRequisitionDto.setUsageInformationLineItems(serviceDtos);
  }

  @Override
  public void update(SiglusRequisitionDto siglusRequisitionDto,
      SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<UsageInformationServiceDto> serviceDtos = siglusRequisitionDto
        .getUsageInformationLineItems();
    siglusRequisitionUpdatedDto.setUsageInformationLineItems(serviceDtos);
    List<UsageInformationLineItem> lineItems = UsageInformationLineItem
        .from(serviceDtos, siglusRequisitionDto.getId());
    log.info("update usage information line items by requisition id: {}",
        siglusRequisitionDto.getId());
    usageInformationLineItemRepository.save(lineItems);
  }

  @Override
  public void delete(UUID requisitionId) {
    log.info("find usage information line items by requisition id: {}", requisitionId);
    List<UsageInformationLineItem> lineItems = usageInformationLineItemRepository
        .findByRequisitionId(requisitionId);
    log.info("delete usage information line items by requisition id: {}", requisitionId);
    usageInformationLineItemRepository.delete(lineItems);
  }

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnableUsageInformation();
  }

  private List<UsageInformationLineItem> createUsageInformationLineItems(
      SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    UsageTemplateColumnSection service = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, SERVICE);
    UsageTemplateColumnSection information = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, INFORMATION);
    List<UUID> kitIds = orderableKitRepository.findAllKitProduct().stream()
        .map(Orderable::getId).collect(toList());
    Set<UUID> orderableIds = siglusRequisitionDto.getAvailableProducts().stream()
        .filter(product -> !kitIds.contains(product.getId()))
        .map(BaseDto::getId)
        .collect(Collectors.toSet());
    List<UsageInformationLineItem> usageInformationLineItems = newArrayList();
    for (UsageTemplateColumn templateServiceColumn : service.getColumns()) {
      if (!Boolean.TRUE.equals(templateServiceColumn.getIsDisplayed())) {
        continue;
      }
      for (UsageTemplateColumn templateInformationColumn : information.getColumns()) {
        if (!Boolean.TRUE.equals(templateInformationColumn.getIsDisplayed())) {
          continue;
        }
        orderableIds.forEach(orderableId ->
            usageInformationLineItems.add(UsageInformationLineItem.builder()
                .requisitionId(siglusRequisitionDto.getId())
                .information(templateInformationColumn.getName())
                .service(templateServiceColumn.getName())
                .orderableId(orderableId)
                .build()));
      }
    }
    return usageInformationLineItems;
  }

}
