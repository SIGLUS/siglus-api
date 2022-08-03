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
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageInformationInformationDto;
import org.siglus.siglusapi.dto.UsageInformationOrderableDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;

@RunWith(MockitoJUnitRunner.class)
public class UsageInformationDataProcessorTest {

  @Captor
  private ArgumentCaptor<List<UsageInformationLineItem>> lineItemsArgumentCaptor;

  @InjectMocks
  private UsageInformationDataProcessor usageInformationDataProcessor;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private UsageInformationLineItemRepository usageInformationLineItemRepository;

  @Mock
  private OrderableKitRepository orderableKitRepository;

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final Integer value = RandomUtils.nextInt();

  private static final String INFORMATION = "information";

  private static final String SERVICE = "service";

  @Test
  public void shouldNotCreateLineItemsWhenInitiateIfDisableUsageInformation() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableUsageInformation(false).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();

    // when
    usageInformationDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(usageInformationLineItemRepository, times(0)).save(lineItemsArgumentCaptor.capture());
  }

  @Test
  public void shouldCreateLineItemsWhenInitiateIfEnableUsageInformationButHaveKitProduct() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableUsageInformation(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    VersionObjectReferenceDto availableProduct = new VersionObjectReferenceDto();
    availableProduct.setId(orderableId);
    VersionObjectReferenceDto availableProduct2 = new VersionObjectReferenceDto();
    availableProduct2.setId(UUID.randomUUID());
    Set<VersionObjectReferenceDto> availableProducts = newHashSet(availableProduct,
        availableProduct2);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    siglusRequisitionDto.setAvailableProducts(availableProducts);
    UsageTemplateColumn serviceColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(SERVICE)
        .build();
    UsageTemplateColumnSection serviceSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.USAGEINFORMATION)
        .name(SERVICE)
        .columns(newArrayList(serviceColumn))
        .build();
    UsageTemplateColumn informationColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(INFORMATION)
        .build();
    UsageTemplateColumnSection informationSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.USAGEINFORMATION)
        .name(INFORMATION)
        .columns(newArrayList(informationColumn))
        .build();
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, SERVICE))
        .thenReturn(serviceSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, INFORMATION))
        .thenReturn(informationSection);
    Orderable kitProduct = new Orderable(Code.code("kitProduct"), Dispensable.createNew("each"),
        10, 7, true, orderableId, 1L);
    when(orderableKitRepository.findAllKitProduct()).thenReturn(Arrays.asList(kitProduct));

    // when
    usageInformationDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(usageInformationLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<UsageInformationLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(INFORMATION, lineItems.get(0).getInformation());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(availableProduct2.getId(), lineItems.get(0).getOrderableId());
    assertNull(lineItems.get(0).getValue());
  }

  @Test
  public void shouldCreateLineItemsWhenInitiateIfEnableUsageInformation() {
    // given
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableUsageInformation(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    VersionObjectReferenceDto availableProduct = new VersionObjectReferenceDto();
    availableProduct.setId(orderableId);
    Set<VersionObjectReferenceDto> availableProducts = newHashSet(availableProduct);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    siglusRequisitionDto.setAvailableProducts(availableProducts);
    UsageTemplateColumn serviceColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(SERVICE)
        .build();
    UsageTemplateColumnSection serviceSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.USAGEINFORMATION)
        .name(SERVICE)
        .columns(newArrayList(serviceColumn))
        .build();
    UsageTemplateColumn informationColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .name(INFORMATION)
        .build();
    UsageTemplateColumnSection informationSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.USAGEINFORMATION)
        .name(INFORMATION)
        .columns(newArrayList(informationColumn))
        .build();
    List<UsageTemplateColumnSection> templateColumnSections = newArrayList();
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, SERVICE))
        .thenReturn(serviceSection);
    when(siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.USAGEINFORMATION, INFORMATION))
        .thenReturn(informationSection);
    when(orderableKitRepository.findAllKitProduct()).thenReturn(Collections.emptyList());

    // when
    usageInformationDataProcessor.initiate(siglusRequisitionDto, templateColumnSections);

    // then
    verify(usageInformationLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<UsageInformationLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(INFORMATION, lineItems.get(0).getInformation());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(orderableId, lineItems.get(0).getOrderableId());
    assertNull(lineItems.get(0).getValue());
  }

  @Test
  public void shouldTransToDtoWhenGetUsageInformation() {
    // given
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    UsageInformationLineItem lineItem = UsageInformationLineItem.builder()
        .requisitionId(requisitionId)
        .service(SERVICE)
        .information(INFORMATION)
        .orderableId(orderableId)
        .value(value)
        .build();
    lineItem.setId(id);
    List<UsageInformationLineItem> lineItems = newArrayList(lineItem);
    when(usageInformationLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    usageInformationDataProcessor.get(siglusRequisitionDto);

    // then
    List<UsageInformationServiceDto> serviceDtos = siglusRequisitionDto
        .getUsageInformationLineItems();
    assertEquals(1, serviceDtos.size());
  }

  @Test
  public void shouldSaveLineItemsWhenUpdate() {
    // given
    UsageInformationOrderableDto usageInformationOrderableDto = UsageInformationOrderableDto
        .builder()
        .usageInformationLineItemId(id)
        .value(value)
        .build();
    Map<UUID, UsageInformationOrderableDto> orderables = newHashMap();
    orderables.put(orderableId, usageInformationOrderableDto);
    UsageInformationInformationDto information = new UsageInformationInformationDto();
    information.setOrderables(orderables);
    Map<String, UsageInformationInformationDto> informations = newHashMap();
    informations.put(INFORMATION, information);
    UsageInformationServiceDto serviceDto = new UsageInformationServiceDto();
    serviceDto.setService(SERVICE);
    serviceDto.setInformations(informations);
    List<UsageInformationServiceDto> serviceDtos = newArrayList(serviceDto);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setUsageInformationLineItems(serviceDtos);
    SiglusRequisitionDto siglusRequisitionUpdatedDto = new SiglusRequisitionDto();

    // when
    usageInformationDataProcessor.update(siglusRequisitionDto, siglusRequisitionUpdatedDto);

    // then
    assertEquals(serviceDtos, siglusRequisitionUpdatedDto.getUsageInformationLineItems());
    verify(usageInformationLineItemRepository).save(lineItemsArgumentCaptor.capture());
    List<UsageInformationLineItem> lineItems = lineItemsArgumentCaptor.getValue();
    assertEquals(1, lineItems.size());
    assertEquals(id, lineItems.get(0).getId());
    assertEquals(requisitionId, lineItems.get(0).getRequisitionId());
    assertEquals(INFORMATION, lineItems.get(0).getInformation());
    assertEquals(SERVICE, lineItems.get(0).getService());
    assertEquals(orderableId, lineItems.get(0).getOrderableId());
    assertEquals(value, lineItems.get(0).getValue());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDeleteByRequisitionId() {
    // given
    List<UsageInformationLineItem> lineItems = newArrayList(
        UsageInformationLineItem.builder().build());
    when(usageInformationLineItemRepository.findByRequisitionId(requisitionId))
        .thenReturn(lineItems);

    // when
    usageInformationDataProcessor.delete(requisitionId);

    // then
    verify(usageInformationLineItemRepository).findByRequisitionId(requisitionId);
    verify(usageInformationLineItemRepository).delete(lineItems);
  }

}
