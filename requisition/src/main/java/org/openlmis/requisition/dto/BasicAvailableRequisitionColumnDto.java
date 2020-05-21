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

package org.openlmis.requisition.dto;

import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.SourceType;

@Getter
@Setter
public class BasicAvailableRequisitionColumnDto extends BaseAvailableRequisitionColumnDto
    implements AvailableRequisitionColumn.Exporter {

  /**
   * Create new instance of AvailableRequisitionColumnDto based
   * on given {@link AvailableRequisitionColumn}.
   *
   * @param column instance of AvailableRequisitionColumn
   * @return new instance of AvailableRequisitionColumnDto.
   */
  public static BasicAvailableRequisitionColumnDto newInstance(
      AvailableRequisitionColumn column) {
    BasicAvailableRequisitionColumnDto columnDto = new BasicAvailableRequisitionColumnDto();
    column.export(columnDto);
    return columnDto;
  }

  @Override
  public void setId(UUID id) {
    // not supported operation
  }

  @Override
  public void setName(String name) {
    // not supported operation
  }

  @Override
  public void setSources(Set<SourceType> sources) {
    // not supported operation
  }

  @Override
  public void setOptions(Set<AvailableRequisitionColumnOptionDto> options) {
    // not supported operation
  }

  @Override
  public void setLabel(String label) {
    // not supported operation
  }

  @Override
  public void setIndicator(String indicator) {
    // not supported operation
  }

  @Override
  public void setMandatory(Boolean mandatory) {
    // not supported operation
  }

  @Override
  public void setIsDisplayRequired(Boolean isDisplayRequired) {
    // not supported operation
  }

  @Override
  public void setCanBeChangedByUser(Boolean canBeChangedByUser) {
    // not supported operation
  }

  @Override
  public void setDefinition(String definition) {
    // not supported operation
  }

  @Override
  public void setSupportsTag(Boolean supportsTag) {
    // not supported operation
  }
}
