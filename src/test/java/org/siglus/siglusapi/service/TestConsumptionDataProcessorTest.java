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
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;

@RunWith(MockitoJUnitRunner.class)
public class TestConsumptionDataProcessorTest {

  @Captor
  private ArgumentCaptor<List<TestConsumptionLineItem>> lineItemsArgumentCaptor;

  @InjectMocks
  private TestConsumptionDataProcessor testConsumptionDataProcessor;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final Integer value = RandomUtils.nextInt();

  private static final String SERVICE = "service";

  private static final String PROJECT = "project";

  private static final String OUTCOME = "outcome";

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID periodId = UUID.randomUUID();

  private final List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

  @Test
  public void shouldNotCreateLineItemsWhenInitiateIfDisableTestConsumption() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableRapidTestConsumption(false).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

    // when
    testConsumptionDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(testConsumptionLineItemRepository, times(0)).save(lineItemsArgumentCaptor.capture());
  }

  @Test
  public void shouldCreateLineItemsWhenInitiateIfEnableTestConsumption() {
    // given
    givenReturn();
    when(siglusUsageReportService.isNotSupplyFacilityOrNotUsageReports(programId, facilityId)).thenReturn(true);

    // when
    testConsumptionDataProcessor.initiate(mockRequisitionDto(), templateColumnSections);

    // then
    verify(testConsumptionLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<TestConsumptionLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(PROJECT, lineItems.get(0).getProject());
    assertEquals(OUTCOME, lineItems.get(0).getOutcome());
    assertNull(lineItems.get(0).getValue());
  }

  @Test
  public void shouldSumValueWhenIsHighLevelFacility() {
    // given
    givenReturn();
    when(siglusUsageReportService.isNotSupplyFacilityOrNotUsageReports(programId, facilityId)).thenReturn(false);
    when(testConsumptionLineItemRepository.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId))
        .thenReturn(singletonList(mockTestConsumptionOutcomeDto(40)));
    when(testConsumptionLineItemRepository.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId))
        .thenReturn(singletonList(mockTestConsumptionOutcomeDto(30)));

    // when
    testConsumptionDataProcessor.initiate(mockRequisitionDto(), templateColumnSections);

    // then
    verify(testConsumptionLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<TestConsumptionLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(70), lineItems.get(0).getValue());
  }

  @Test
  public void shouldGetTestConsumptionSection() {
    // given
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    TestConsumptionLineItem lineItem = TestConsumptionLineItem.builder()
        .requisitionId(requisitionId)
        .service(SERVICE)
        .project(PROJECT)
        .outcome(OUTCOME)
        .value(value)
        .build();
    lineItem.setId(id);
    List<TestConsumptionLineItem> lineItems = newArrayList(lineItem);
    when(testConsumptionLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    testConsumptionDataProcessor.get(siglusRequisitionDto);

    // then
    List<TestConsumptionServiceDto> serviceDtos = siglusRequisitionDto
        .getTestConsumptionLineItems();
    assertEquals(1, serviceDtos.size());
    TestConsumptionOutcomeDto outcomeDto =
        serviceDtos.get(0).getProjects().get(PROJECT).getOutcomes().get(OUTCOME);
    assertEquals(id, outcomeDto.getTestConsumptionLineItemId());
    assertEquals(OUTCOME, outcomeDto.getOutcome());
    assertEquals(value, outcomeDto.getValue());
  }

  @Test
  public void shouldSaveLineItemsWhenUpdate() {
    // given
    TestConsumptionOutcomeDto outcomeDto = TestConsumptionOutcomeDto
        .builder()
        .testConsumptionLineItemId(id)
        .outcome(OUTCOME)
        .value(value)
        .build();
    Map<String, TestConsumptionOutcomeDto> outcomeMap = newHashMap();
    outcomeMap.put(OUTCOME, outcomeDto);
    TestConsumptionProjectDto projectDto = new TestConsumptionProjectDto();
    projectDto.setProject(PROJECT);
    projectDto.setOutcomes(outcomeMap);
    Map<String, TestConsumptionProjectDto> projectMap = new HashMap<>();
    projectMap.put(PROJECT, projectDto);
    TestConsumptionServiceDto serviceDto = new TestConsumptionServiceDto();
    serviceDto.setService(SERVICE);
    serviceDto.setProjects(projectMap);

    List<TestConsumptionServiceDto> serviceDtos = newArrayList(serviceDto);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTestConsumptionLineItems(serviceDtos);
    SiglusRequisitionDto siglusRequisitionUpdatedDto = new SiglusRequisitionDto();

    // when
    testConsumptionDataProcessor.update(siglusRequisitionDto, siglusRequisitionUpdatedDto);

    // then
    assertEquals(serviceDtos, siglusRequisitionUpdatedDto.getTestConsumptionLineItems());
    verify(testConsumptionLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<TestConsumptionLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(id, lineItems.get(0).getId());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(PROJECT, lineItems.get(0).getProject());
    assertEquals(OUTCOME, lineItems.get(0).getOutcome());
    assertEquals(value, lineItems.get(0).getValue());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDeleteByRequisitionId() {
    // given
    List<TestConsumptionLineItem> lineItems = newArrayList(
        TestConsumptionLineItem.builder().build());
    when(testConsumptionLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    testConsumptionDataProcessor.delete(requisitionId);

    // then
    verify(testConsumptionLineItemRepository).findByRequisitionId(requisitionId);
    verify(testConsumptionLineItemRepository).delete(lineItems);
  }

  private TestConsumptionOutcomeDto mockTestConsumptionOutcomeDto(Integer value) {
    return TestConsumptionOutcomeDto.builder()
        .outcome(OUTCOME)
        .service(SERVICE)
        .project(PROJECT)
        .value(value)
        .build();
  }

  private void givenReturn() {
    UsageTemplateColumn serviceColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(SERVICE)
        .build();
    UsageTemplateColumnSection serviceSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.RAPIDTESTCONSUMPTION)
        .name(SERVICE)
        .columns(newArrayList(serviceColumn))
        .build();
    UsageTemplateColumn projectColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(PROJECT)
        .build();
    UsageTemplateColumnSection projectSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.RAPIDTESTCONSUMPTION)
        .name(PROJECT)
        .columns(newArrayList(projectColumn))
        .build();
    UsageTemplateColumn outcomeColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(OUTCOME)
        .build();
    UsageTemplateColumnSection outcomeSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.RAPIDTESTCONSUMPTION)
        .name(OUTCOME)
        .columns(newArrayList(outcomeColumn))
        .build();

    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, SERVICE))
        .thenReturn(serviceSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, PROJECT))
        .thenReturn(projectSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.RAPIDTESTCONSUMPTION, OUTCOME))
        .thenReturn(outcomeSection);
  }

  private SiglusRequisitionDto mockRequisitionDto() {
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableRapidTestConsumption(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    siglusRequisitionDto.setFacility(new ObjectReferenceDto(facilityId));
    siglusRequisitionDto.setProgram(new ObjectReferenceDto(programId));
    siglusRequisitionDto.setProcessingPeriod(new ObjectReferenceDto(periodId));
    return siglusRequisitionDto;
  }

}

