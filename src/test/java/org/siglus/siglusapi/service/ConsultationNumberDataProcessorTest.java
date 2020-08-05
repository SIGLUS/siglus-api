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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.service.mapper.ConsultationNumberLineItemMapper;

@RunWith(MockitoJUnitRunner.class)
public class ConsultationNumberDataProcessorTest {

  @InjectMocks
  private ConsultationNumberDataProcessor processor;

  @Mock
  private ConsultationNumberLineItemRepository repo;

  @Mock
  private ConsultationNumberLineItemMapper mapper;

  @Captor
  private ArgumentCaptor<List<ConsultationNumberLineItem>> captor;

  private UUID requisitionId = UUID.randomUUID();

  @Test
  public void shouldReturnFalseWhenIsDisabledGivenNotEnableConsultationNumber() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    requisition.setTemplate(template);
    RequisitionTemplateExtensionDto extension = new RequisitionTemplateExtensionDto();
    template.setExtension(extension);
    extension.setEnableConsultationNumber(false);

    // when
    boolean disabled = processor.isDisabled(requisition);

    // then
    assertTrue(disabled);
  }

  @Test
  public void shouldReturnFalseWhenIsDisabledGivenEnableConsultationNumber() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    requisition.setTemplate(template);
    RequisitionTemplateExtensionDto extension = new RequisitionTemplateExtensionDto();
    template.setExtension(extension);
    extension.setEnableConsultationNumber(true);

    // when
    boolean disabled = processor.isDisabled(requisition);

    // then
    assertFalse(disabled);
  }

  @Test
  public void shouldCallFindAndMapperWhenDoInitiate() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    requisition.setId(requisitionId);
    UsageTemplateColumnSection nonMatchedTemplate = new UsageTemplateColumnSection();
    nonMatchedTemplate.setCategory(UsageCategory.KITUSAGE);
    UsageTemplateColumnSection matchedTemplate = new UsageTemplateColumnSection();
    matchedTemplate.setCategory(UsageCategory.CONSULTATIONNUMBER);
    String groupName = "cnSection";
    matchedTemplate.setName(groupName);
    UsageTemplateColumn column = new UsageTemplateColumn();
    String columnName = "cnColumn";
    column.setName(columnName);
    column.setIsDisplayed(true);
    matchedTemplate.setColumns(singletonList(column));
    List<ConsultationNumberLineItem> savedLineItems =
        singletonList(mock(ConsultationNumberLineItem.class));
    when(repo.save(anyListOf(ConsultationNumberLineItem.class))).thenReturn(savedLineItems);
    List<ConsultationNumberGroupDto> mappedGroups = singletonList(
        mock(ConsultationNumberGroupDto.class));
    when(mapper.fromLineItems(savedLineItems)).thenReturn(mappedGroups);

    // when
    processor.doInitiate(requisition, asList(nonMatchedTemplate, matchedTemplate));

    // then
    assertEquals(mappedGroups, requisition.getConsultationNumberLineItems());
    verify(repo).save(captor.capture());
    List<ConsultationNumberLineItem> lineItemsToSave = captor.getValue();
    assertEquals(1, lineItemsToSave.size());
    ConsultationNumberLineItem lineItemToSave = lineItemsToSave.get(0);
    assertEquals(requisitionId, lineItemToSave.getRequisitionId());
    assertEquals(groupName, lineItemToSave.getGroup());
    assertEquals(columnName, lineItemToSave.getColumn());
  }

  @Test
  public void shouldCallFindAndMapperWhenGet() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    requisition.setId(requisitionId);
    List<ConsultationNumberLineItem> lineItems =
        singletonList(mock(ConsultationNumberLineItem.class));
    when(repo.findByRequisitionId(requisitionId)).thenReturn(lineItems);
    List<ConsultationNumberGroupDto> mappedGroups = singletonList(
        mock(ConsultationNumberGroupDto.class));
    when(mapper.fromLineItems(lineItems)).thenReturn(mappedGroups);

    // when
    processor.get(requisition);

    // then
    assertEquals(mappedGroups, requisition.getConsultationNumberLineItems());
  }

  @Test
  public void shouldCallMapperAndSaveWhenUpdate() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    requisition.setId(requisitionId);
    List<ConsultationNumberGroupDto> groupDtos = singletonList(
        mock(ConsultationNumberGroupDto.class));
    requisition.setConsultationNumberLineItems(groupDtos);
    ConsultationNumberLineItem lineItem = mock(ConsultationNumberLineItem.class);
    when(mapper.fromGroups(groupDtos)).thenReturn(singletonList(lineItem));
    List<ConsultationNumberLineItem> savedLineItems =
        singletonList(mock(ConsultationNumberLineItem.class));
    when(repo.save(anyListOf(ConsultationNumberLineItem.class))).thenReturn(savedLineItems);
    List<ConsultationNumberGroupDto> mappedGroups = singletonList(
        mock(ConsultationNumberGroupDto.class));
    when(mapper.fromLineItems(savedLineItems)).thenReturn(mappedGroups);
    SiglusRequisitionDto requisitionUpdated = new SiglusRequisitionDto();

    // when
    processor.update(requisition, requisitionUpdated);

    // then
    verify(repo).save(captor.capture());
    List<ConsultationNumberLineItem> lineItemsToUpdate = captor.getValue();
    assertThat(lineItemsToUpdate, hasItems(lineItem));
    verify(lineItem).setRequisitionId(requisitionId);
    assertEquals(mappedGroups, requisitionUpdated.getConsultationNumberLineItems());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDelete() {
    // given
    List<ConsultationNumberLineItem> lineItems =
        singletonList(mock(ConsultationNumberLineItem.class));
    when(repo.findByRequisitionId(requisitionId)).thenReturn(lineItems);

    // when
    processor.delete(requisitionId);

    // then
    verify(repo).findByRequisitionId(requisitionId);
    verify(repo).delete(lineItems);
  }

}
