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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.AgeGroupLineItemDto;
import org.siglus.siglusapi.dto.AgeGroupServiceDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;

@RunWith(MockitoJUnitRunner.class)
public class TestAgeGroupDataProcessor {

  @InjectMocks
  private AgeGroupDataProcessor ageGroupDataProcessor;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;
  @Mock
  private AgeGroupLineItemRepository ageGroupLineItemRepository;
  @Captor
  private ArgumentCaptor<List<AgeGroupLineItem>> lineItemsArgumentCaptor;

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID periodId = UUID.randomUUID();

  private static final String SERVICE = "service";
  private static final String GROUP = "group";
  private final Integer value = RandomUtils.nextInt();
  private final UUID id = UUID.randomUUID();
  private final List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

  @Test
  public void shouldNotCreateLineItemsWhenInitiateIfDisableAgeGroup() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableAgeGroup(false).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

    // when
    ageGroupDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(ageGroupLineItemRepository, times(0)).save(lineItemsArgumentCaptor.capture());
  }

  @Test
  public void shouldCreateLineItemsWhenInitiateIfEnableAgeGroup() {
    // given
    givenReturn();
    Mockito.when(siglusUsageReportService.isNonTopLevelOrNotUsageReports(any(), any())).thenReturn(true);

    // when
    ageGroupDataProcessor.initiate(mockRequisitionDto(), templateColumnSections);

    // then
    verify(ageGroupLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<AgeGroupLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(GROUP, lineItems.get(0).getGroup());
    assertNull(lineItems.get(0).getValue());
  }

  @Test
  public void shouldSumValueWhenIsHighLevelFacility() {
    // given
    givenReturn();
    Mockito.when(siglusUsageReportService.isNonTopLevelOrNotUsageReports(any(), any())).thenReturn(false);
    Mockito.when(ageGroupLineItemRepository.sumValueRequisitionsUnderHighLevelFacility(facilityId, periodId, programId))
        .thenReturn(singletonList(mockAgeGroupLineItemDto(10)));
    Mockito.when(ageGroupLineItemRepository.maxValueRequisitionsInLastPeriods(facilityId, periodId, programId))
        .thenReturn(singletonList(mockAgeGroupLineItemDto(20)));

    // when
    ageGroupDataProcessor.initiate(mockRequisitionDto(), templateColumnSections);

    // then
    verify(ageGroupLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<AgeGroupLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(30), lineItems.get(0).getValue());
  }

  @Test
  public void shouldGetAgeGroup() {
    // given
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    AgeGroupLineItem lineItem = AgeGroupLineItem.builder()
        .requisitionId(requisitionId)
        .service(SERVICE)
        .group(GROUP)
        .value(value)
        .build();
    lineItem.setId(id);
    List<AgeGroupLineItem> lineItems = newArrayList(lineItem);
    when(ageGroupLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    ageGroupDataProcessor.get(siglusRequisitionDto);

    // then
    List<AgeGroupServiceDto> serviceDtos = siglusRequisitionDto
        .getAgeGroupLineItems();
    assertEquals(1, serviceDtos.size());
    AgeGroupLineItemDto ageGroupLineItemDto =
        serviceDtos.get(0).getColumns().get(GROUP);
    assertEquals(id, ageGroupLineItemDto.getAgeGroupLineItemId());
    assertEquals(value, ageGroupLineItemDto.getValue());
  }

  @Test
  public void shouldSaveLineItemsWhenUpdate() {
    // given
    AgeGroupLineItemDto lineItemDto = AgeGroupLineItemDto
        .builder()
        .ageGroupLineItemId(id)
        .value(value)
        .build();
    Map<String, AgeGroupLineItemDto> groupMap = newHashMap();
    groupMap.put(GROUP, lineItemDto);
    AgeGroupServiceDto serviceDto = new AgeGroupServiceDto();
    serviceDto.setService(SERVICE);
    serviceDto.setColumns(groupMap);

    List<AgeGroupServiceDto> serviceDtos = newArrayList(serviceDto);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setAgeGroupLineItems(serviceDtos);
    SiglusRequisitionDto siglusRequisitionUpdatedDto = new SiglusRequisitionDto();

    when(ageGroupLineItemRepository.save((Iterable<AgeGroupLineItem>) any()))
        .thenAnswer(i -> i.getArguments()[0]);

    // when
    ageGroupDataProcessor.update(siglusRequisitionDto, siglusRequisitionUpdatedDto);

    // then
    assertEquals(serviceDtos, siglusRequisitionUpdatedDto.getAgeGroupLineItems());
    verify(ageGroupLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<AgeGroupLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(id, lineItems.get(0).getId());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(GROUP, lineItems.get(0).getGroup());
    assertEquals(value, lineItems.get(0).getValue());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDeleteByRequisitionId() {
    // when
    ageGroupDataProcessor.delete(requisitionId);

    // then
    verify(ageGroupLineItemRepository).deleteByRequisitionId(requisitionId);
  }

  private AgeGroupLineItemDto mockAgeGroupLineItemDto(Integer value) {
    return AgeGroupLineItemDto.builder()
        .service(SERVICE)
        .group(GROUP)
        .value(value)
        .build();
  }

  private void givenReturn() {
    UsageTemplateColumn serviceColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(SERVICE)
        .build();
    UsageTemplateColumnSection serviceSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.AGEGROUP)
        .name(SERVICE)
        .columns(newArrayList(serviceColumn))
        .build();
    UsageTemplateColumn groupColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(GROUP)
        .build();
    UsageTemplateColumnSection groupSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.AGEGROUP)
        .name(GROUP)
        .columns(newArrayList(groupColumn))
        .build();

    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.AGEGROUP, SERVICE))
        .thenReturn(serviceSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.AGEGROUP, GROUP))
        .thenReturn(groupSection);
  }

  private SiglusRequisitionDto mockRequisitionDto() {
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableAgeGroup(true).build();
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
