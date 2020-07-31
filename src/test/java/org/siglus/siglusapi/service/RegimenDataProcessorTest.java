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
import static com.google.common.collect.Sets.newHashSet;
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
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;

@RunWith(MockitoJUnitRunner.class)
public class RegimenDataProcessorTest {

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private RegimenLineItemRepository regimenLineItemRepository;

  @Mock
  private RequisitionService requisitionService;

  @Captor
  private ArgumentCaptor<List<RegimenLineItem>> lineItemsArgumentCaptor;

  @InjectMocks
  private RegimenDataProcessor regimenDataProcessor;

  private static final String REGIMEN = "regimen";

  private static final String PATIENTS = "patients";

  private UUID requisitionId = UUID.randomUUID();

  private UUID regimenId1 = UUID.randomUUID();

  private UUID regimenId2 = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID templateId = UUID.randomUUID();

  private UUID id = UUID.randomUUID();

  private Integer value = RandomUtils.nextInt();

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
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableRegimen(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setId(templateId);
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);

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

    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.REGIMEN, REGIMEN))
        .thenReturn(regimenSection);
    when(regimenRepository.findAll())
        .thenReturn(newArrayList(mockCustomRegimen(),mockNoCustomRegimen()));
    when(regimenRepository.findAllByProgramIdInAndActiveTrue(any()))
        .thenReturn(newArrayList(mockNoCustomRegimen()));
    when(regimenRepository.findAllByProgramIdInAndActiveTrueAndIsCustomIsTrue(any()))
        .thenReturn(newArrayList(mockCustomRegimen()));
    when(requisitionService.getAssociateProgram(any())).thenReturn(newHashSet(programId));

    // when
    regimenDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(regimenLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<RegimenLineItem> regimenLineItems = lineItemsArgumentCaptor.getValue();

    assertEquals(1, regimenLineItems.size());
    RegimenLineItem lineItem = regimenLineItems.get(0);
    assertEquals(regimenId1, lineItem.getRegimenId());
    assertEquals(PATIENTS, lineItem.getColumn());
    assertNull(lineItem.getValue());

    assertEquals(1, siglusRequisitionDto.getCustomRegimens().size());
    assertEquals(regimenId2, siglusRequisitionDto.getCustomRegimens().get(0).getId());
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
        .regimenId(regimenId1).column(PATIENTS)
        .value(value)
        .build();
    List<RegimenLineItem> lineItems = newArrayList(lineItem);
    when(regimenLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);
    when(regimenRepository.findAll())
        .thenReturn(newArrayList(mockCustomRegimen(),mockNoCustomRegimen()));
    when(requisitionService.getAssociateProgram(any())).thenReturn(newHashSet(programId));

    // when
    regimenDataProcessor.get(siglusRequisitionDto);

    // then
    List<RegimenLineDto> lineDtos = siglusRequisitionDto
        .getRegimenLineItems();
    assertEquals(1, lineDtos.size());
    RegimenColumnDto columnDto = lineDtos.get(0).getColumns().get(PATIENTS);
    assertEquals(value, columnDto.getValue());
    assertEquals(regimenId1, lineDtos.get(0).getRegimen().getId());
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

    List<RegimenLineDto> lineDtos = newArrayList(lineDto);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setRegimenLineItems(lineDtos);
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
  }

  @Test
  public void shouldCallFindAndDeleteWhenDeleteByRequisitionId() {
    // given
    List<RegimenLineItem> lineItems = newArrayList(new RegimenLineItem());
    when(regimenLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    regimenDataProcessor.delete(requisitionId);

    // then
    verify(regimenLineItemRepository).findByRequisitionId(requisitionId);
    verify(regimenLineItemRepository).delete(lineItems);
  }

  private Regimen mockNoCustomRegimen() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId1);
    regimen.setCode(Code.code("ABC+3TC+RAL+DRV+RTV"));
    regimen.setIsCustom(false);
    return regimen;
  }

  private Regimen mockCustomRegimen() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId2);
    regimen.setCode(Code.code("ABC+3TC+DTG"));
    regimen.setIsCustom(true);
    return regimen;
  }
}
