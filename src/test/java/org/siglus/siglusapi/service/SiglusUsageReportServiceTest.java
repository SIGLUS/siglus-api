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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.domain.AvailableUsageColumnSection;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.testutils.ProgramExtensionDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SiglusUsageReportServiceTest {

  static final String  USER_INPUT = "USER_INPUT";

  @Mock
  UsageTemplateColumnSectionRepository columnSectionRepository;

  @Mock
  OrderableKitRepository orderableKitRepository;

  @Mock
  StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Mock
  ProgramExtensionRepository programExtensionRepository;

  @Mock
  KitUsageLineItemRepository kitUsageRepository;

  @InjectMocks
  SiglusUsageReportService siglusUsageReportService;

  private RequisitionV2Dto requisitionV2Dto;

  private UUID templateId;

  private UUID programId;

  private UUID requisitionId = UUID.randomUUID();

  private RequisitionTemplateExtensionDto extensionDto;

  private BasicRequisitionTemplateDto requisitionTemplateDto;

  @Before
  public void prepare() {
    templateId = UUID.randomUUID();
    requisitionV2Dto = new RequisitionV2Dto();
    programId = UUID.randomUUID();

    extensionDto = new RequisitionTemplateExtensionDto();
    extensionDto.setEnableKitUsage(false);
    requisitionTemplateDto = new BasicRequisitionTemplateDto();
    requisitionTemplateDto.setId(templateId);
    requisitionTemplateDto.setAssociatePrograms(new HashSet<>());
    requisitionTemplateDto.setExtension(extensionDto);
    requisitionV2Dto.setTemplate(requisitionTemplateDto);
    requisitionV2Dto.setId(requisitionId);
    when(columnSectionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(getMockKitSection());
    Orderable kitProduct = new Orderable(Code.code("kitProduct"), null, 10, 7,
        true, UUID.randomUUID(), 1L);
    ProgramOrderable programOrderable = new ProgramOrderable();
    Program program = new Program(programId);
    programOrderable.setProgram(program);
    kitProduct.setProgramOrderables(Arrays.asList(programOrderable));
    when(orderableKitRepository.findAllKitProduct()).thenReturn(Arrays.asList(kitProduct));
    KitUsageLineItem usageLineItem = KitUsageLineItem.builder()
        .requisitionId(requisitionId)
        .collection("collection")
        .service("service")
        .build();
    when(kitUsageRepository.save(Arrays.asList(any(KitUsageLineItem.class)))).thenReturn(
        Arrays.asList(usageLineItem)
    );
    ProgramExtension programExtension = new ProgramExtensionDataBuilder()
        .withProgramId(programId)
        .withParentId(null)
        .build();

    programExtension.setIsVirtual(true);
    when(programExtensionRepository.findAll()).thenReturn(newArrayList(programExtension));
    when(stockCardRangeSummaryStockManagementService.findAll()).thenReturn(new ArrayList<>());
  }

  @Test
  public void shouldIsEmptyIfTemplateEnableKitFalse() {
    SiglusRequisitionDto requisitionDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);
    assertEquals(0, requisitionDto.getKitUsageLineItems().size());
  }

  @Test
  public void shouldHaveTwoValueIfTemplateEnableKitTrue() {
    extensionDto.setEnableKitUsage(true);
    requisitionTemplateDto.setExtension(extensionDto);
    SiglusRequisitionDto requisitionDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);
    assertEquals(1, requisitionDto.getKitUsageLineItems().size());
  }

  private List<UsageTemplateColumnSection> getMockKitSection() {
    UsageTemplateColumnSection templateColumnSection = new UsageTemplateColumnSection();
    templateColumnSection.setCategory(UsageCategory.KITUSAGE);
    templateColumnSection.setName("collection");

    AvailableUsageColumnSection availableSection = new AvailableUsageColumnSection();
    availableSection.setName("collection");
    availableSection.setId(UUID.randomUUID());
    availableSection.setColumns(getAvailableUsageColumns());
    availableSection.setDisplayOrder(1);
    templateColumnSection.setSection(availableSection);

    UsageTemplateColumn column = new UsageTemplateColumn();
    column.setId(UUID.randomUUID());
    column.setName("kitColumn");
    column.setSource(USER_INPUT);
    column.setIsDisplayed(true);
    column.setDisplayOrder(1);
    column.setAvailableSources(USER_INPUT);
    templateColumnSection.setColumns(Arrays.asList(column));

    UsageTemplateColumnSection templateServiceSection = new UsageTemplateColumnSection();
    templateServiceSection.setCategory(UsageCategory.KITUSAGE);
    templateServiceSection.setName("service");
    templateServiceSection.setSection(availableSection);

    UsageTemplateColumn service = new UsageTemplateColumn();
    service.setId(UUID.randomUUID());
    service.setName("HF");
    service.setSource(USER_INPUT);
    service.setIsDisplayed(true);
    service.setDisplayOrder(1);
    service.setAvailableSources(USER_INPUT);
    templateServiceSection.setColumns(Arrays.asList(service));

    return Arrays.asList(templateColumnSection, templateServiceSection);
  }


  private List<AvailableUsageColumn> getAvailableUsageColumns() {
    AvailableUsageColumn availableUsageColumn = new AvailableUsageColumn();
    availableUsageColumn.setId(UUID.randomUUID());
    availableUsageColumn.setName("available");
    availableUsageColumn.setSources("USER_INPUT|STOCK_CARDS");
    availableUsageColumn.setDisplayOrder(1);
    return Arrays.asList(availableUsageColumn);
  }

}
