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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.javers.common.collections.Sets;
import org.openlmis.requisition.dto.RequisitionTemplateColumnDto;
import org.openlmis.requisition.testutils.AvailableRequisitionColumnDataBuilder;
import org.openlmis.requisition.testutils.AvailableRequisitionColumnOptionDataBuilder;
import org.openlmis.requisition.testutils.api.DataBuilder;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionTemplateColumnDataBuilder implements
    DataBuilder<RequisitionTemplateColumn>, DtoDataBuilder<RequisitionTemplateColumnDto> {

  private String name;
  private String label;
  private String indicator;
  private int displayOrder;
  private Boolean isDisplayed;
  private SourceType source;
  private AvailableRequisitionColumn columnDefinition;
  private AvailableRequisitionColumnOption option;
  private String definition;
  private String tag;

  /**
   * Builder for {@link RequisitionTemplateColumn}.
   */
  public RequisitionTemplateColumnDataBuilder() {
    name = "column";
    label = "Column";
    indicator = "C";
    displayOrder = 1;
    isDisplayed = true;
    source = SourceType.REFERENCE_DATA;
    option = new AvailableRequisitionColumnOptionDataBuilder().build();
    columnDefinition = new AvailableRequisitionColumnDataBuilder()
        .withOptions(Sets.asSet(option))
        .build();
    definition = "Some column";
  }

  /**
   * Builds {@link RequisitionTemplateColumn} instance with test data.
   */
  @Override
  public RequisitionTemplateColumn build() {
    if (null != columnDefinition) {
      Optional
          .ofNullable(columnDefinition.getOptions())
          .orElse(Collections.emptySet())
          .forEach(option -> option.setRequisitionColumn(columnDefinition));
    }
    return new RequisitionTemplateColumn(name, label, indicator, displayOrder, isDisplayed, source,
        columnDefinition, option, definition, tag);
  }

  /**
   * Builds {@link RequisitionTemplateColumnDto} instance with test data.
   */
  @Override
  public RequisitionTemplateColumnDto buildAsDto() {
    RequisitionTemplateColumn requisitionTemplateColumn = build();
    RequisitionTemplateColumnDto requisitionTemplateColumnDto = new RequisitionTemplateColumnDto();
    requisitionTemplateColumn.export(requisitionTemplateColumnDto);
    return requisitionTemplateColumnDto;
  }

  public RequisitionTemplateColumnDataBuilder withNotDisplayed() {
    this.isDisplayed = false;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withDisplay(Boolean display) {
    this.isDisplayed = display;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withIndicator(String indicator) {
    this.indicator = indicator;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withColumnDefinition(
      AvailableRequisitionColumn columnDefinition) {
    this.columnDefinition = columnDefinition;
    return this;
  }

  /**
   * Set column definition with parameters.
   */
  public RequisitionTemplateColumnDataBuilder withColumnDefinition(String name, String indicator,
                                                                   Set<SourceType> sources) {
    columnDefinition = new AvailableRequisitionColumn();
    columnDefinition.setName(name);
    columnDefinition.setIndicator(indicator);
    columnDefinition.setSources(sources);

    return this;
  }

  public RequisitionTemplateColumnDataBuilder withSource(SourceType source) {
    this.source = source;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withOption(AvailableRequisitionColumnOption option) {
    this.option = option;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withoutOption() {
    this.option = null;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withLabel(String label) {
    this.label = label;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withTag(String tag) {
    this.tag = tag;
    return this;
  }

  public RequisitionTemplateColumnDataBuilder withDefinition(String definition) {
    this.definition = definition;
    return this;
  }
}
