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

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;

@Getter
@Setter
public class BasicRequisitionTemplateColumnDto extends BaseRequisitionTemplateColumnDto
    implements RequisitionTemplateColumn.Exporter {

  private BasicAvailableRequisitionColumnDto columnDefinition;

  /**
   * Create new map of RequisitionTemplateColumnDto based on given list
   * of {@link RequisitionTemplateColumn}.
   *
   * @param columns list of {@link RequisitionTemplateColumn}
   * @return new map of RequisitionTemplateColumn.
   */
  public static Map<String, BasicRequisitionTemplateColumnDto> newInstance(
      Map<String, RequisitionTemplateColumn> columns) {

    Map<String, BasicRequisitionTemplateColumnDto> columnDtos = new HashMap<>();
    columns.forEach((key, column) -> columnDtos.put(key, newInstance(column)));
    return columnDtos;
  }

  /**
   * Create new instance of RequisitionTemplateColumnDto based
   * on given {@link RequisitionTemplateColumn}.
   *
   * @param column instance of RequisitionTemplateColumn
   * @return new instance of RequisitionTemplateColumnDto.
   */
  public static BasicRequisitionTemplateColumnDto newInstance(
      RequisitionTemplateColumn column) {
    if (column == null) {
      return null;
    }
    BasicRequisitionTemplateColumnDto requisitionTemplateDto =
        new BasicRequisitionTemplateColumnDto();
    column.export(requisitionTemplateDto);
    requisitionTemplateDto.setColumnDefinition(
        BasicAvailableRequisitionColumnDto.newInstance(column.getColumnDefinition()));

    return requisitionTemplateDto;
  }
}
