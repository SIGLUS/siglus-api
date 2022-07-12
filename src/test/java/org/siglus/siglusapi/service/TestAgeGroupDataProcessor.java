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
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;

@RunWith(MockitoJUnitRunner.class)
public class TestAgeGroupDataProcessor {

  @InjectMocks
  private AgeGroupDataProcessor ageGroupDataProcessor;

  @Mock
  private AgeGroupLineItemRepository ageGroupLineItemRepository;
  private final UUID requisitionId = UUID.randomUUID();

  private static List<UsageTemplateColumnSection> templateColumnSections;

  private static SiglusRequisitionDto siglusRequisitionDto;

  @Before
  public void setup() {
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableAgeGroup(false).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setTemplate(template);
    templateColumnSections = newArrayList();
    UsageTemplateColumn serviceColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .label(FieldConstants.SECTION_SERVICE_LABEL)
        .build();
    UsageTemplateColumnSection serviceSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.AGEGROUP)
        .label(FieldConstants.SECTION_SERVICE_LABEL)
        .columns(newArrayList(serviceColumn))
        .build();
    UsageTemplateColumn ageGroupColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .label(FieldConstants.AGE_GROUP_LABEL)
        .build();
    UsageTemplateColumnSection ageGroupSection = UsageTemplateColumnSection.builder()
        .category(UsageCategory.AGEGROUP)
        .label(FieldConstants.AGE_GROUP_LABEL)
        .columns(newArrayList(ageGroupColumn))
        .build();
    UsageTemplateColumn otherColumn = UsageTemplateColumn.builder()
        .isDisplayed(true)
        .label(FieldConstants.AGE_GROUP_LABEL)
        .build();
    UsageTemplateColumnSection otherSection = UsageTemplateColumnSection.builder()
        .label(FieldConstants.AGE_GROUP_LABEL)
        .category(UsageCategory.KITUSAGE)
        .columns(newArrayList(otherColumn))
        .build();
    templateColumnSections.add(serviceSection);
    templateColumnSections.add(ageGroupSection);
    templateColumnSections.add(otherSection);
    when(ageGroupLineItemRepository.save(anyList())).thenReturn(newArrayList(new AgeGroupLineItem()));
    when(ageGroupLineItemRepository.findByRequisitionId(any())).thenReturn(newArrayList(new AgeGroupLineItem()));
    when(ageGroupLineItemRepository.save(anyList())).thenReturn(newArrayList(new AgeGroupLineItem()));
  }

  @Test
  public void shouldCreateLineItemsWhenInitiate() {
    ageGroupDataProcessor.doInitiate(siglusRequisitionDto, templateColumnSections);
    assertEquals(siglusRequisitionDto.getAgeGroupLineItems().size(), 1);
  }

  @Test
  public void shouldGetAgeGroupByRequisitionId() {
    ageGroupDataProcessor.get(siglusRequisitionDto);
    assertEquals(siglusRequisitionDto.getAgeGroupLineItems().size(), 1);
  }

  @Test
  public void shouldReturnAgeGroupIsDisable() {
    boolean disabled = ageGroupDataProcessor.isDisabled(siglusRequisitionDto);
    assertEquals(disabled, true);
  }

  @Test
  public void shouldReturnAgeGroupEnsable() {
    RequisitionTemplateExtensionDto extension = RequisitionTemplateExtensionDto.builder()
        .enableAgeGroup(true).build();
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(extension);
    siglusRequisitionDto.setTemplate(template);
    boolean disabled = ageGroupDataProcessor.isDisabled(siglusRequisitionDto);
    assertEquals(disabled, false);
  }

  @Test
  public void shouldDeteleAgeGroupByRequisitionId() {
    ageGroupDataProcessor.delete(requisitionId);
    verify(ageGroupLineItemRepository).deleteByRequisitionId(requisitionId);
  }

  @Test
  public void shouldUpdateAgeGroupLineItem() {
    ageGroupDataProcessor.update(new SiglusRequisitionDto(), siglusRequisitionDto);
    assertEquals(siglusRequisitionDto.getAgeGroupLineItems().size(), 1);
  }
}
