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
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;

@RunWith(MockitoJUnitRunner.class)
public class PatientDataProcessorTest {

  @InjectMocks
  private PatientDataProcessor processor;

  @Mock
  private PatientLineItemRepository repo;

  @Mock
  private PatientLineItemMapper mapper;

  @Captor
  private ArgumentCaptor<List<PatientLineItem>> captor;

  private final UUID requisitionId = UUID.randomUUID();

  @Test
  public void shouldReturnFalseWhenIsDisabledGivenNotEnablePatientLineItems() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    requisition.setTemplate(template);
    RequisitionTemplateExtensionDto extension = new RequisitionTemplateExtensionDto();
    template.setExtension(extension);
    extension.setEnablePatientLineItem(false);

    // when
    boolean disabled = processor.isDisabled(requisition);

    // then
    assertTrue(disabled);
  }

  @Test
  public void shouldReturnFalseWhenIsDisabledGivenEnablePatientLineItems() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    requisition.setTemplate(template);
    RequisitionTemplateExtensionDto extension = new RequisitionTemplateExtensionDto();
    template.setExtension(extension);
    extension.setEnablePatientLineItem(true);

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
    UsageTemplateColumnSection nonPatientTemplate = new UsageTemplateColumnSection();
    nonPatientTemplate.setCategory(UsageCategory.KITUSAGE);
    UsageTemplateColumnSection patientTemplate = new UsageTemplateColumnSection();
    patientTemplate.setCategory(UsageCategory.PATIENT);
    String patientName = "patientSection";
    patientTemplate.setName(patientName);
    UsageTemplateColumn patientColumn = new UsageTemplateColumn();
    String columnName = "patientColumn";
    patientColumn.setName(columnName);
    patientColumn.setIsDisplayed(true);
    patientTemplate.setColumns(singletonList(patientColumn));
    List<PatientLineItem> savedLineItems = singletonList(mock(PatientLineItem.class));
    when(repo.save(anyListOf(PatientLineItem.class))).thenReturn(savedLineItems);
    List<PatientGroupDto> mappedGroups = singletonList(mock(PatientGroupDto.class));
    when(mapper.from(savedLineItems)).thenReturn(mappedGroups);

    // when
    processor.doInitiate(requisition, asList(nonPatientTemplate, patientTemplate));

    // then
    assertEquals(mappedGroups, requisition.getPatientLineItems());
    verify(repo).save(captor.capture());
    List<PatientLineItem> lineItemsToSave = captor.getValue();
    assertEquals(1, lineItemsToSave.size());
    PatientLineItem lineItemToSave = lineItemsToSave.get(0);
    assertEquals(requisitionId, lineItemToSave.getRequisitionId());
    assertEquals(patientName, lineItemToSave.getGroup());
    assertEquals(columnName, lineItemToSave.getColumn());
  }

  @Test
  public void shouldCallFindAndMapperWhenGet() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    requisition.setId(requisitionId);
    List<PatientLineItem> lineItems = singletonList(mock(PatientLineItem.class));
    when(repo.findByRequisitionId(requisitionId)).thenReturn(lineItems);
    List<PatientGroupDto> patientGroupDtos = singletonList(mock(PatientGroupDto.class));
    when(mapper.from(lineItems)).thenReturn(patientGroupDtos);

    // when
    processor.get(requisition);

    // then
    assertEquals(patientGroupDtos, requisition.getPatientLineItems());
  }

  @Test
  public void shouldCallMapperAndSaveWhenUpdate() {
    // given
    SiglusRequisitionDto requisition = new SiglusRequisitionDto();
    requisition.setId(requisitionId);
    PatientGroupDto patientGroupDto = mock(PatientGroupDto.class);
    List<PatientGroupDto> lineItems = singletonList(patientGroupDto);
    requisition.setPatientLineItems(lineItems);
    PatientLineItem patientLineItem = mock(PatientLineItem.class);
    when(mapper.from(patientGroupDto)).thenReturn(singletonList(patientLineItem));
    List<PatientLineItem> savedLineItems = singletonList(mock(PatientLineItem.class));
    when(repo.save(anyListOf(PatientLineItem.class))).thenReturn(savedLineItems);
    List<PatientGroupDto> mappedGroups = singletonList(mock(PatientGroupDto.class));
    when(mapper.from(savedLineItems)).thenReturn(mappedGroups);
    SiglusRequisitionDto requisitionUpdated = new SiglusRequisitionDto();

    // when
    processor.update(requisition, requisitionUpdated);

    // then
    verify(repo).save(captor.capture());
    List<PatientLineItem> lineItemsToUpdate = captor.getValue();
    assertThat(lineItemsToUpdate, hasItems(patientLineItem));
    verify(patientLineItem).setRequisitionId(requisitionId);
    assertEquals(mappedGroups, requisitionUpdated.getPatientLineItems());
  }

  @Test
  public void shouldCallFindAndDeleteWhenDelete() {
    // given
    List<PatientLineItem> lineItems = singletonList(mock(PatientLineItem.class));
    when(repo.findByRequisitionId(requisitionId)).thenReturn(lineItems);

    // when
    processor.delete(requisitionId);

    // then
    verify(repo).findByRequisitionId(requisitionId);
    verify(repo).delete(lineItems);
  }

}
