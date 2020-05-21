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

package org.openlmis.requisition.domain;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openlmis.requisition.domain.RequisitionTemplate.ORDER_RELATED_COLUMNS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_CANNOT_ASSIGN_TEMPLATE_TO_SEVERAL_PROGRAMS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_MAP_TAGS_DUPLICATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_TAG_NOT_SET;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.requisition.dto.RequisitionTemplateColumnDto;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.openlmis.requisition.exception.ValidationMessageException;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionTemplateTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private RequisitionTemplate requisitionTemplate;

  private static final String[] COLUMN_NAMES = {
      CALCULATED_ORDER_QUANTITY, REQUESTED_QUANTITY, REQUESTED_QUANTITY_EXPLANATION
  };

  private static final List<String> columnsWithTags = Lists.newArrayList(
      TOTAL_CONSUMED_QUANTITY, TOTAL_LOSSES_AND_ADJUSTMENTS, TOTAL_RECEIVED_QUANTITY);

  @Before
  public void setUp() {
    requisitionTemplate = new RequisitionTemplateDataBuilder()
        .withRequiredColumns()
        .build();
  }

  @Test
  public void testChangeColumnDisplayOrderToLower() {
    requisitionTemplate.changeColumnDisplayOrder(COLUMN_NAMES[2], 1);
    Map<String, RequisitionTemplateColumn> mapAfterChange = requisitionTemplate.viewColumns();
    Assert.assertEquals(1, mapAfterChange.get(COLUMN_NAMES[2]).getDisplayOrder());
    Assert.assertEquals(2, mapAfterChange.get(COLUMN_NAMES[0]).getDisplayOrder());
    Assert.assertEquals(3, mapAfterChange.get(COLUMN_NAMES[1]).getDisplayOrder());
  }

  @Test
  public void testChangeColumnDisplayOrderToHigher() {
    requisitionTemplate.changeColumnDisplayOrder(COLUMN_NAMES[0], 3);
    Map<String, RequisitionTemplateColumn> mapAfterChange = requisitionTemplate.viewColumns();
    Assert.assertEquals(1, mapAfterChange.get(COLUMN_NAMES[1]).getDisplayOrder());
    Assert.assertEquals(2, mapAfterChange.get(COLUMN_NAMES[2]).getDisplayOrder());
    Assert.assertEquals(3, mapAfterChange.get(COLUMN_NAMES[0]).getDisplayOrder());
  }

  @Test
  public void testChangeColumnDisplayOrderToTheSame() {
    requisitionTemplate.changeColumnDisplayOrder(COLUMN_NAMES[1], 2);
    Map<String, RequisitionTemplateColumn> mapAfterChange = requisitionTemplate.viewColumns();
    Assert.assertEquals(1, mapAfterChange.get(COLUMN_NAMES[0]).getDisplayOrder());
    Assert.assertEquals(2, mapAfterChange.get(COLUMN_NAMES[1]).getDisplayOrder());
    Assert.assertEquals(3, mapAfterChange.get(COLUMN_NAMES[2]).getDisplayOrder());
  }

  @Test
  public void shouldCheckIfItHasColumnsDefined() {
    assertTrue(requisitionTemplate.hasColumnsDefined());
    assertFalse(new RequisitionTemplate(new HashMap<>()).hasColumnsDefined());
  }

  @Test
  public void shouldThrowIfSourceIsNotAvailableInColumn() {
    expected.expect(ValidationMessageException.class);
    requisitionTemplate.changeColumnSource(COLUMN_NAMES[0], SourceType.REFERENCE_DATA);

    expected.expectMessage(RequisitionTemplate.SOURCE + SourceType.REFERENCE_DATA
        + RequisitionTemplate.WARNING_SUFFIX);
  }

  @Test
  public void shouldThrowIfOptionIsNotAvailableInColumn() {
    expected.expect(ValidationMessageException.class);
    AvailableRequisitionColumnOption option = new AvailableRequisitionColumnOption(
        requisitionTemplate.viewColumns().get(COLUMN_NAMES[0])
            .getColumnDefinition(), "option1", "label1");
    requisitionTemplate.changeColumnOption(COLUMN_NAMES[0], option);

    expected.expectMessage(RequisitionTemplate.OPTION + option.getOptionName()
        + RequisitionTemplate.WARNING_SUFFIX);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(RequisitionTemplate.class)
        .withRedefinedSuperclass()
        .withPrefabValues(RequisitionTemplateColumn.class,
            new RequisitionTemplateColumnDataBuilder().build(),
            new RequisitionTemplateColumnDataBuilder().withName("another-column").build())
        .withPrefabValues(RequisitionTemplateAssignment.class,
            new RequisitionTemplateAssignment(UUID.randomUUID(), UUID.randomUUID(), null),
            new RequisitionTemplateAssignment(UUID.randomUUID(), UUID.randomUUID(), null))
        .withIgnoredFields("id", "createdDate", "modifiedDate", "programId", "facilityTypeIds")
        .verify();
  }

  @Test
  public void shouldExportRequisitionTemplate() {
    RequisitionTemplateDto templateDto = new RequisitionTemplateDto();
    requisitionTemplate.export(templateDto);

    assertEquals(requisitionTemplate.getId(), templateDto.getId());
    assertEquals(requisitionTemplate.getCreatedDate(), templateDto.getCreatedDate());
    assertEquals(requisitionTemplate.getModifiedDate(), templateDto.getModifiedDate());
    assertEquals(requisitionTemplate.isPopulateStockOnHandFromStockCards(),
        templateDto.isPopulateStockOnHandFromStockCards());
    assertEquals(requisitionTemplate.getNumberOfPeriodsToAverage(),
        templateDto.getNumberOfPeriodsToAverage());
  }

  @Test
  public void shouldImportRequisitionTemplate() {
    RequisitionTemplateDto templateDto = new RequisitionTemplateDto();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withRequiredColumns()
        .build();
    template.export(templateDto);
    setColumns(templateDto, template);

    RequisitionTemplate newTemplate = RequisitionTemplate.newInstance(templateDto, columnsWithTags);

    assertEquals(templateDto.getId(), newTemplate.getId());
    assertEquals(templateDto.getCreatedDate(), newTemplate.getCreatedDate());
    assertEquals(templateDto.getModifiedDate(), newTemplate.getModifiedDate());
    assertEquals(templateDto.isPopulateStockOnHandFromStockCards(),
        newTemplate.isPopulateStockOnHandFromStockCards());
    assertEquals(templateDto.getNumberOfPeriodsToAverage(),
        newTemplate.getNumberOfPeriodsToAverage());
    newTemplate.viewColumns().entrySet()
        .forEach(requisitionTemplateColumnEntry ->
            assertEquals(template.viewColumns()
                    .get(requisitionTemplateColumnEntry.getKey()).getName(),
                requisitionTemplateColumnEntry.getValue().getName()));
    assertEquals(templateDto.getId(), newTemplate.getId());
    assertThat(newTemplate.getProgramId(), is(templateDto.getProgramId()));
    assertThat(
        newTemplate.getFacilityTypeIds(),
        hasItems(templateDto.getFacilityTypeIds().toArray(new UUID[0]))
    );
  }

  @Test
  public void shouldAddAssignment() {
    UUID programId = UUID.randomUUID();
    Set<UUID> facilityTypeId = IntStream
        .range(0, 4)
        .mapToObj(idx -> UUID.randomUUID())
        .collect(Collectors.toSet());

    RequisitionTemplate template = new RequisitionTemplate();
    facilityTypeId.forEach(id -> template.addAssignment(programId, id));

    assertThat(template.getProgramId(), is(programId));
    assertThat(template.getFacilityTypeIds(), hasSize(facilityTypeId.size()));
    assertThat(template.getFacilityTypeIds(), hasItems(facilityTypeId.toArray(new UUID[0])));
  }

  @Test
  public void shouldThrowExceptionIfTryToAssignToSeveralPrograms() {
    expected.expect(ValidationMessageException.class);
    expected.expectMessage(containsString(ERROR_CANNOT_ASSIGN_TEMPLATE_TO_SEVERAL_PROGRAMS));

    RequisitionTemplate template = new RequisitionTemplate();
    template.addAssignment(UUID.randomUUID(), null);
    template.addAssignment(UUID.randomUUID(), null);
  }

  @Test
  public void shouldFacilityTypeIdsShouldNotContainNullValue() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withAssignment(UUID.randomUUID(), null)
        .build();

    assertThat(template.getFacilityTypeIds(), hasSize(0));
  }

  @Test
  public void shouldContainCorrectAssignmentsAfterUpdate() {
    UUID programId = UUID.randomUUID();
    UUID[] facilityTypeIds = IntStream
        .range(0, 10)
        .mapToObj(idx -> UUID.randomUUID())
        .toArray(UUID[]::new);

    // those two arrays should contain some same and different UUIDs to verify that:
    // * it is possible to remove assignments (like facilityTypeIds[3])
    // * it is possible to add assignments (like facilityTypeIds[1])
    // * existing assignments should not be removed (like facilityTypeIds[0])
    final UUID[] facilityTypeIds1 = new UUID[]{
        facilityTypeIds[0], facilityTypeIds[3], facilityTypeIds[5], facilityTypeIds[7]
    };
    final UUID[] facilityTypeIds2 = new UUID[]{
        facilityTypeIds[0], facilityTypeIds[1], facilityTypeIds[2], facilityTypeIds[4],
        facilityTypeIds[6], facilityTypeIds[7], facilityTypeIds[8], facilityTypeIds[9]
    };

    RequisitionTemplateDataBuilder templateBuilder1 = new RequisitionTemplateDataBuilder();
    Arrays
        .stream(facilityTypeIds1)
        .forEach(facilityTypeId -> templateBuilder1.withAssignment(programId, facilityTypeId));

    RequisitionTemplate template = templateBuilder1.build();
    assertThat(template.getProgramId(), is(programId));
    assertThat(template.getFacilityTypeIds(), hasSize(facilityTypeIds1.length));
    assertThat(template.getFacilityTypeIds(), containsInAnyOrder(facilityTypeIds1));

    RequisitionTemplateDataBuilder templateBuilder2 = new RequisitionTemplateDataBuilder();
    Arrays
        .stream(facilityTypeIds2)
        .forEach(facilityTypeId -> templateBuilder2.withAssignment(programId, facilityTypeId));

    template.updateFrom(templateBuilder2.build());
    assertThat(template.getProgramId(), is(programId));
    assertThat(template.getFacilityTypeIds(), hasSize(facilityTypeIds2.length));
    assertThat(template.getFacilityTypeIds(), containsInAnyOrder(facilityTypeIds2));
  }

  @Test
  public void shouldNotAllowDuplicateTags() {
    expected.expect(ValidationMessageException.class);

    RequisitionTemplateDto templateDto = new RequisitionTemplateDto();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards()
        .withDuplicatedTag()
        .build();

    template.export(templateDto);
    setColumns(templateDto, template);

    RequisitionTemplate.newInstance(templateDto, columnsWithTags);

    expected.expectMessage(ERROR_COLUMNS_MAP_TAGS_DUPLICATED);
  }

  @Test
  public void shouldEnforceTagsSetForColumnsThatRequireThem() {
    expected.expect(ValidationMessageException.class);

    RequisitionTemplateDto templateDto = new RequisitionTemplateDto();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards()
        .withAllColumns()
        .build();

    template.export(templateDto);
    setColumns(templateDto, template);

    RequisitionTemplate.newInstance(templateDto, columnsWithTags);

    expected.expectMessage(containsString(ERROR_COLUMNS_TAG_NOT_SET));
  }

  @Test
  public void shouldNotEnforceTagsWhenTemplateIsNotStockBased() {
    RequisitionTemplateDto templateDto = new RequisitionTemplateDto();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .build();

    template.export(templateDto);
    setColumns(templateDto, template);

    // This shouldn't throw exception, even though we don't have tags
    RequisitionTemplate.newInstance(templateDto, columnsWithTags);
  }

  @Test
  public void hideOrderColumnsShouldHideOrderColumnsFromMap() {
    //given
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .build();
    
    //when
    template.hideOrderRelatedColumns();
    
    //then
    for (Map.Entry<String, RequisitionTemplateColumn> entry : template.viewColumns().entrySet()) {
      if (ORDER_RELATED_COLUMNS.contains(entry.getKey())) {
        assertFalse(entry.getValue().getIsDisplayed());
      }
    }
  }
  
  @Test
  public void copyConstructorShouldReturnEquivalentTemplate() {
    //given
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .build();
    
    //when
    RequisitionTemplate templateCopy = new RequisitionTemplate(template);
    
    assertEquals(template, templateCopy);
  }

  private void setColumns(RequisitionTemplateDto templateDto, RequisitionTemplate template) {
    templateDto.setColumnsMap(template.viewColumns().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
          RequisitionTemplateColumnDto dto = new RequisitionTemplateColumnDto();
          entry.getValue().export(dto);
          return dto;
        })));
  }
}
