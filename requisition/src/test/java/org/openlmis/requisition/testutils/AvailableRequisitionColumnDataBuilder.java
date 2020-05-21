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

package org.openlmis.requisition.testutils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.AvailableRequisitionColumnOption;
import org.openlmis.requisition.domain.ColumnType;
import org.openlmis.requisition.domain.SourceType;
import org.openlmis.requisition.testutils.api.DataBuilder;

public class AvailableRequisitionColumnDataBuilder implements
    DataBuilder<AvailableRequisitionColumn> {

  private UUID id;
  private String name;
  private Set<SourceType> sources;
  private Set<AvailableRequisitionColumnOption> options;
  private String label;
  private String indicator;
  private Boolean mandatory;
  private Boolean isDisplayRequired;
  private Boolean canChangeOrder;
  private Boolean canBeChangedByUser;
  private Boolean supportsTags;
  private String definition;
  private ColumnType columnType;

  /**
   * Builder for {@link AvailableRequisitionColumn}.
   */
  public AvailableRequisitionColumnDataBuilder() {
    id = UUID.randomUUID();
    name = "column";
    sources = Sets.asSet(SourceType.REFERENCE_DATA);
    options = Sets.asSet(new AvailableRequisitionColumnOptionDataBuilder().build());
    label = "Column";
    indicator = "C";
    mandatory = false;
    isDisplayRequired = false;
    canChangeOrder = true;
    canBeChangedByUser = true;
    supportsTags = false;
    definition = "Some column";
    columnType = ColumnType.TEXT;
  }

  /**
   * Builds {@link AvailableRequisitionColumn} instance with test data.
   */
  public AvailableRequisitionColumn build() {
    AvailableRequisitionColumn column = new AvailableRequisitionColumn(name, sources, options,
        label, indicator, mandatory, isDisplayRequired, canChangeOrder, canBeChangedByUser,
        supportsTags, definition, columnType);
    column.setId(id);
    column.getOptions()
        .forEach(option -> option.setRequisitionColumn(column));
    return column;
  }

  public AvailableRequisitionColumnDataBuilder withOptions(
      Set<AvailableRequisitionColumnOption> options) {
    this.options = options;
    return this;
  }

  public AvailableRequisitionColumnDataBuilder withoutOptions() {
    this.options = Collections.emptySet();
    return this;
  }

  public AvailableRequisitionColumnDataBuilder withSources(Set<SourceType> sources) {
    this.sources = sources;
    return this;
  }

  public AvailableRequisitionColumnDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public AvailableRequisitionColumnDataBuilder withoutId() {
    this.id = null;
    return this;
  }
}
