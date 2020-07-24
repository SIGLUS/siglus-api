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

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestConsumptionDataProcessor implements UsageReportDataProcessor {

  private static final String SERVICE = "service";

  private static final String PROJECT = "project";

  private static final String OUTCOME = "outcome";

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;

  @Override
  public void doInitiate(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    List<TestConsumptionLineItem> testConsumptionLineItems =
        createTestConsumptionLineItems(siglusRequisitionDto, templateColumnSections);
    log.info("save test consumption line items by requisition id: {}",
        siglusRequisitionDto.getId());

    List<TestConsumptionLineItem> saved =
        testConsumptionLineItemRepository.save(testConsumptionLineItems);
    List<TestConsumptionServiceDto> serviceDtos = TestConsumptionServiceDto.from(saved);
    siglusRequisitionDto.setTestConsumptionLineItems(serviceDtos);
  }

  @Override
  public void get(SiglusRequisitionDto siglusRequisitionDto) {
    List<TestConsumptionLineItem> testConsumptionLineItems =
        testConsumptionLineItemRepository.findByRequisitionId(siglusRequisitionDto.getId());
    List<TestConsumptionServiceDto> serviceDtos =
        TestConsumptionServiceDto.from(testConsumptionLineItems);

    siglusRequisitionDto.setTestConsumptionLineItems(serviceDtos);
  }

  @Override
  public void update(SiglusRequisitionDto siglusRequisitionDto,
      SiglusRequisitionDto siglusRequisitionUpdatedDto) {

    List<TestConsumptionServiceDto> serviceDtos =
        siglusRequisitionDto.getTestConsumptionLineItems();
    siglusRequisitionUpdatedDto.setTestConsumptionLineItems(serviceDtos);

    List<TestConsumptionLineItem> lineItems =
        TestConsumptionLineItem.from(serviceDtos, siglusRequisitionDto.getId());
    log.info("update test consumption line items by requisition id: {}",
        siglusRequisitionDto.getId());
    testConsumptionLineItemRepository.save(lineItems);
  }

  @Override
  public void delete(UUID requisitionId) {
    List<TestConsumptionLineItem> testConsumptionLineItems =
        testConsumptionLineItemRepository.findByRequisitionId(requisitionId);
    log.info("delete test consumption line items by requisition id: {}", requisitionId);
    testConsumptionLineItemRepository.delete(testConsumptionLineItems);
  }

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnableRapidTestConsumption();
  }

  private List<TestConsumptionLineItem> createTestConsumptionLineItems(
      SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    UsageTemplateColumnSection service = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, SERVICE);
    UsageTemplateColumnSection project = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, PROJECT);
    UsageTemplateColumnSection outcome = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, OUTCOME);

    List<TestConsumptionLineItem> testConsumptionLineItems = newArrayList();

    for (UsageTemplateColumn templateServiceColumn : service.getColumns()) {
      if (!Boolean.TRUE.equals(templateServiceColumn.getIsDisplayed())) {
        continue;
      }

      for (UsageTemplateColumn templateProjectColumn : project.getColumns()) {
        if (!Boolean.TRUE.equals(templateProjectColumn.getIsDisplayed())) {
          continue;
        }

        for (UsageTemplateColumn templateOutcomeColumn : outcome.getColumns()) {
          if (!Boolean.TRUE.equals(templateOutcomeColumn.getIsDisplayed())) {
            continue;
          }

          testConsumptionLineItems.add(TestConsumptionLineItem.builder()
              .requisitionId(siglusRequisitionDto.getId())
              .outcome(templateOutcomeColumn.getName())
              .project(templateProjectColumn.getName())
              .service(templateServiceColumn.getName())
              .build());

        }
      }

    }

    return testConsumptionLineItems;
  }
}
