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

import static org.junit.Assert.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.javers.common.collections.Sets;
import org.junit.Test;
import org.openlmis.requisition.dto.RequisitionTemplateColumnDto;

public class RequisitionTemplateColumnTest {

  @Test
  public void shouldExportRequisitionTemplateColumn() {
    RequisitionTemplateColumn column = new RequisitionTemplateColumnDataBuilder()
        .withTag("sampleTag")
        .build();

    RequisitionTemplateColumnDto dto = new RequisitionTemplateColumnDto();
    column.export(dto);

    assertValues(dto, column);
  }

  @Test
  public void shouldImportRequisitionTemplateColumn() {
    RequisitionTemplateColumnDto dto = new RequisitionTemplateColumnDataBuilder().buildAsDto();
    RequisitionTemplateColumn column = RequisitionTemplateColumn.newInstance(dto);

    assertValues(dto, column);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(RequisitionTemplateColumn.class)
        .withPrefabValues(AvailableRequisitionColumn.class,
            new AvailableRequisitionColumn("column1", Sets.asSet(SourceType.CALCULATED),
                Sets.asSet(new AvailableRequisitionColumnOption()), "label1", "A", true, true,
                true,true, true, "definition1", ColumnType.NUMERIC),
            new AvailableRequisitionColumn("column2", Sets.asSet(SourceType.USER_INPUT),
                Sets.asSet(new AvailableRequisitionColumnOption()), "label2", "B", false,false,
                false, false, false, "definition2", ColumnType.TEXT))
        .withPrefabValues(AvailableRequisitionColumnOption.class,
            new AvailableRequisitionColumnOption(null, "optionName1", "optionLabel1"),
            new AvailableRequisitionColumnOption(null, "optionName2", "optionLabel2"))
        .verify();
  }

  private void assertValues(RequisitionTemplateColumnDto dto, RequisitionTemplateColumn column) {
    assertEquals(column.getName(), dto.getName());
    assertEquals(column.getSource(), dto.getSource());
    assertEquals(column.getOption().getOptionName(), dto.getOption().getOptionName());
    assertEquals(column.getDisplayOrder(), dto.getDisplayOrder());
    assertEquals(column.getIsDisplayed(), dto.getIsDisplayed());
    assertEquals(column.getLabel(), dto.getLabel());
    assertEquals(column.getTag(), dto.getTag());
  }
}
