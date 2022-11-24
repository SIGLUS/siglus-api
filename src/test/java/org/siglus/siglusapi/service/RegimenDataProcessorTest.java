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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
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
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;

@RunWith(MockitoJUnitRunner.class)
public class RegimenDataProcessorTest {

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private RegimenLineItemRepository regimenLineItemRepository;

  @Mock
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private UsageTemplateColumnSectionRepository columnSectionRepository;

  @Captor
  private ArgumentCaptor<List<RegimenLineItem>> lineItemsArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<RegimenSummaryLineItem>> summaryArgumentCaptor;

  @InjectMocks
  private RegimenDataProcessor regimenDataProcessor;

  private static final String REGIMEN = "regimen";

  private static final String SUMMARY = "summary";

  private static final String PATIENTS = "patients";

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID regimenId1 = UUID.randomUUID();

  private final UUID regimenId2 = UUID.randomUUID();

  private final String rowName = "row";

  private final UUID templateId = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final Integer value = RandomUtils.nextInt();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID periodId = UUID.randomUUID();

  private final List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

  @Test
  public void shouldNotCreateLineItemsWhenInitiateIfDisableRegimen() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableRegimen(false).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

    // when
    regimenDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(regimenLineItemRepository, times(0)).save(lineItemsArgumentCaptor.capture());
  }

  @Test
  public void shouldCreateLineItemsWhenInitiateIfEnableRegimen() {
    // given
    givenReturn();
    when(siglusUsageReportService.isNotSupplyFacilityOrNotUsageReports(programId, facilityId)).thenReturn(true);
    SiglusRequisitionDto siglusRequisitionDto = mockRequisitionDto();

    // when
    regimenDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(regimenLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<RegimenLineItem> regimenLineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(2, regimenLineItems.size());
    RegimenLineItem lineItem = regimenLineItems.get(0);
    assertEquals(regimenId1, lineItem.getRegimenId());
    assertEquals(PATIENTS, lineItem.getColumn());
    assertNull(lineItem.getValue());
    RegimenLineItem totalLineItem = regimenLineItems.get(1);
    assertNull(totalLineItem.getRegimenId());
    assertEquals(PATIENTS, totalLineItem.getColumn());
    assertNull(totalLineItem.getValue());

    verify(regimenSummaryLineItemRepository).save(summaryArgumentCaptor.capture());
    List<RegimenSummaryLineItem> summaryLineItems = summaryArgumentCaptor.getValue();
    assertEquals(1, summaryLineItems.size());
    RegimenSummaryLineItem summaryLineItem = summaryLineItems.get(0);
    assertEquals(rowName, summaryLineItem.getName());
    assertEquals(PATIENTS, summaryLineItem.getColumn());
    assertNull(summaryLineItem.getValue());

    assertEquals(1, siglusRequisitionDto.getCustomRegimens().size());
    assertEquals(regimenId2, siglusRequisitionDto.getCustomRegimens().get(0).getId());
  }

  @Test
  public void shouldSumValueWhenIsHighLevelFacility() {
    // given
    givenReturn();
    when(siglusUsageReportService.isNotSupplyFacilityOrNotUsageReports(programId, facilityId)).thenReturn(false);
    when(regimenLineItemRepository.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId))
        .thenReturn(asList(mockRegimenColumnDto(regimenId1, 10), mockRegimenColumnDto(null, 10),
            mockRegimenColumnDto(regimenId2, 10)));
    when(regimenLineItemRepository.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId))
        .thenReturn(singletonList(mockRegimenColumnDto(regimenId1, 20)));
    when(regimenSummaryLineItemRepository.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId))
        .thenReturn(singletonList(mockRegimenSummaryLineDto(30)));
    when(regimenSummaryLineItemRepository.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId))
        .thenReturn(singletonList(mockRegimenSummaryLineDto(20)));
    SiglusRequisitionDto siglusRequisitionDto = mockRequisitionDto();

    // when
    regimenDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(regimenLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<RegimenLineItem> regimenLineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(30), regimenLineItems.get(0).getValue());
    assertEquals(Integer.valueOf(10), regimenLineItems.get(1).getValue());
    assertEquals(Integer.valueOf(10), regimenLineItems.get(2).getValue());

    verify(regimenSummaryLineItemRepository).save(summaryArgumentCaptor.capture());
    List<RegimenSummaryLineItem> summaryLineItems = summaryArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(50), summaryLineItems.get(0).getValue());
  }

  @Test
  public void shouldGetRegimenSection() {
    // given
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(templateId);
    siglusRequisitionDto.setTemplate(templateDto);
    RegimenLineItem lineItem = RegimenLineItem.builder()
        .requisitionId(requisitionId)
        .regimenId(regimenId1)
        .column(PATIENTS)
        .value(value)
        .build();
    List<RegimenLineItem> lineItems = newArrayList(lineItem);
    when(regimenLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);
    RegimenSummaryLineItem summaryLineItem = RegimenSummaryLineItem.builder()
        .requisitionId(requisitionId)
        .name(rowName)
        .column(PATIENTS)
        .value(value)
        .build();
    List<RegimenSummaryLineItem> summaryLineItems = newArrayList(summaryLineItem);
    when(regimenSummaryLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(summaryLineItems);
    when(columnSectionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(newArrayList(mockUsageTemplateColumnSection()));
    when(siglusUsageReportService
        .getColumnSection(newArrayList(mockUsageTemplateColumnSection()),
            UsageCategory.REGIMEN, SUMMARY))
        .thenReturn(mockUsageTemplateColumnSection());
    when(regimenRepository.findAll())
        .thenReturn(newArrayList(mockCustomRegimen(), mockNoCustomRegimen()));
    when(requisitionRepository.findOne(requisitionId)).thenReturn(mockRequisition());

    // when
    regimenDataProcessor.get(siglusRequisitionDto);

    // then
    List<RegimenLineDto> lineDtos = siglusRequisitionDto
        .getRegimenLineItems();
    assertEquals(1, lineDtos.size());
    RegimenColumnDto columnDto = lineDtos.get(0).getColumns().get(PATIENTS);
    assertEquals(value, columnDto.getValue());
    assertEquals(regimenId1, lineDtos.get(0).getRegimen().getId());

    List<RegimenSummaryLineDto> dispatchLineDtos = siglusRequisitionDto
        .getRegimenSummaryLineItems();
    assertEquals(1, dispatchLineDtos.size());
    RegimenColumnDto columnDto1 = dispatchLineDtos.get(0).getColumns().get(PATIENTS);
    assertEquals(value, columnDto1.getValue());
    assertEquals(rowName, dispatchLineDtos.get(0).getName());
  }

  @Test
  public void shouldSaveLineItemsWhenUpdate() {
    // given
    RegimenColumnDto columnDto = RegimenColumnDto
        .builder()
        .id(id)
        .value(value)
        .build();
    Map<String, RegimenColumnDto> columnDtoMap = new HashMap<>();
    columnDtoMap.put(PATIENTS, columnDto);
    RegimenDto regimenDto = new RegimenDto();
    regimenDto.setId(regimenId1);
    RegimenLineDto lineDto = new RegimenLineDto();
    lineDto.setRegimen(regimenDto);
    lineDto.setColumns(columnDtoMap);
    RegimenSummaryLineDto summaryLineDto = new RegimenSummaryLineDto();
    summaryLineDto.setName(rowName);
    summaryLineDto.setColumns(columnDtoMap);

    List<RegimenLineDto> lineDtos = newArrayList(lineDto);
    List<RegimenSummaryLineDto> summaryLineDtos = newArrayList(summaryLineDto);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setRegimenLineItems(lineDtos);
    siglusRequisitionDto.setRegimenSummaryLineItems(summaryLineDtos);
    SiglusRequisitionDto siglusRequisitionUpdatedDto = new SiglusRequisitionDto();

    // when
    regimenDataProcessor.update(siglusRequisitionDto, siglusRequisitionUpdatedDto);

    // then
    verify(regimenLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<RegimenLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(id, lineItems.get(0).getId());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(regimenId1, lineItems.get(0).getRegimenId());
    assertEquals(PATIENTS, lineItems.get(0).getColumn());
    assertEquals(value, lineItems.get(0).getValue());

    verify(regimenSummaryLineItemRepository).save(summaryArgumentCaptor.capture());
    List<RegimenSummaryLineItem> summaryLineItems = summaryArgumentCaptor.getValue();
    assertEquals(1, summaryLineItems.size());
    assertEquals(id, summaryLineItems.get(0).getId());
    assertEquals(requisitionId, summaryLineItems.get(0).getRequisitionId());
    assertEquals(rowName, summaryLineItems.get(0).getName());
    assertEquals(PATIENTS, summaryLineItems.get(0).getColumn());
    assertEquals(value, summaryLineItems.get(0).getValue());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDeleteByRequisitionId() {
    // given
    List<RegimenLineItem> lineItems = newArrayList(new RegimenLineItem());
    when(regimenLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);
    List<RegimenSummaryLineItem> summaryLineItems =
        newArrayList(new RegimenSummaryLineItem());
    when(regimenSummaryLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(summaryLineItems);

    // when
    regimenDataProcessor.delete(requisitionId);

    // then
    verify(regimenLineItemRepository).findByRequisitionId(requisitionId);
    verify(regimenLineItemRepository).delete(lineItems);
    verify(regimenSummaryLineItemRepository).findByRequisitionId(requisitionId);
    verify(regimenSummaryLineItemRepository).delete(summaryLineItems);
  }

  private void givenReturn() {
    UsageTemplateColumn regimenColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(PATIENTS)
        .source("USER_INPUT")
        .build();

    UsageTemplateColumnSection regimenSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.REGIMEN)
        .name(REGIMEN)
        .columns(newArrayList(regimenColumn))
        .build();

    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.REGIMEN, REGIMEN))
        .thenReturn(regimenSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.REGIMEN, SUMMARY))
        .thenReturn(mockUsageTemplateColumnSection());
    when(regimenRepository.findAll())
        .thenReturn(newArrayList(mockCustomRegimen(), mockNoCustomRegimen()));
    when(regimenRepository.findAllByProgramIdAndIsAndroidTrueAndIsCustomFalse(any()))
        .thenReturn(newArrayList(mockNoCustomRegimen()));
    when(regimenRepository.findAllByProgramIdAndIsAndroidTrueAndIsCustomTrue(any()))
        .thenReturn(newArrayList(mockCustomRegimen()));
    when(regimenRepository.findAllByProgramIdAndIsAndroidTrue(any()))
        .thenReturn(newArrayList(mockNoCustomRegimen(), mockCustomRegimen()));
    when(regimenRepository.findAllByProgramIdAndActiveTrue(any()))
        .thenReturn(newArrayList(mockCustomRegimen(), mockNoCustomRegimen()));
    when(siglusUsageReportService
        .getColumnSection(newArrayList(mockUsageTemplateColumnSection()),
            UsageCategory.REGIMEN, SUMMARY))
        .thenReturn(mockUsageTemplateColumnSection());
    when(columnSectionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(newArrayList(mockUsageTemplateColumnSection()));
    when(requisitionRepository.findOne(requisitionId)).thenReturn(mockRequisition());
  }

  private Regimen mockNoCustomRegimen() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId1);
    regimen.setCode("ABC+3TC+RAL+DRV+RTV");
    regimen.setIsCustom(false);
    return regimen;
  }

  private Regimen mockCustomRegimen() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId2);
    regimen.setCode("ABC+3TC+DTG");
    regimen.setIsCustom(true);
    return regimen;
  }

  private Requisition mockRequisition() {
    Requisition requisition = new Requisition();
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(templateId);
    requisition.setTemplate(template);
    return requisition;
  }

  private UsageTemplateColumnSection mockUsageTemplateColumnSection() {
    return UsageTemplateColumnSection.builder()
        .name(SUMMARY)
        .label(SUMMARY)
        .requisitionTemplateId(templateId)
        .columns(newArrayList(mockUsageTemplateColumn()))
        .build();
  }

  private UsageTemplateColumn mockUsageTemplateColumn() {
    return UsageTemplateColumn.builder()
        .requisitionTemplateId(templateId)
        .name(rowName)
        .build();
  }

  private SiglusRequisitionDto mockRequisitionDto() {
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableRegimen(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setId(templateId);
    template.setExtension(extension);
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setId(requisitionId);
    requisitionDto.setTemplate(template);
    requisitionDto.setFacility(new ObjectReferenceDto(facilityId));
    requisitionDto.setProgram(new ObjectReferenceDto(programId));
    requisitionDto.setProcessingPeriod(new ObjectReferenceDto(periodId));
    return requisitionDto;
  }

  private RegimenColumnDto mockRegimenColumnDto(UUID regimenId, Integer value) {
    return RegimenColumnDto.builder()
        .regimenId(regimenId)
        .column(PATIENTS)
        .value(value)
        .build();
  }

  private RegimenSummaryLineDto mockRegimenSummaryLineDto(Integer value) {
    RegimenSummaryLineDto regimenSummaryLineDto = new RegimenSummaryLineDto();
    regimenSummaryLineDto.setName(rowName);
    regimenSummaryLineDto.setRegimenSummaryLineColumn(PATIENTS);
    regimenSummaryLineDto.setValue(value);
    return regimenSummaryLineDto;
  }
}
