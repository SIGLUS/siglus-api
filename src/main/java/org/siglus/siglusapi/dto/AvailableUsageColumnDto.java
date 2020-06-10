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

package org.siglus.siglusapi.dto;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.ColumnType;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.springframework.beans.BeanUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailableUsageColumnDto {

  private UUID id;

  private String name;

  private List<String> sources;

  private String label;

  private String indicator;

  private Boolean mandatory;

  private Boolean isDisplayRequired;

  private Boolean canBeChangedByUser;

  private Boolean supportsTag;

  private String definition;

  private Boolean canChangeOrder;

  private ColumnType columnType;

  private AvailableUsageColumnSectionDto section;

  private Integer displayOrder;

  /**
   * Create new instance of AvailableRequisitionColumnDto based
   * on given {@link AvailableRequisitionColumn}.
   *
   * @param column instance of AvailableRequisitionColumn
   * @return new instance of AvailableRequisitionColumnDto.
   */
  public static AvailableUsageColumnDto newInstance(AvailableUsageColumn column) {
    AvailableUsageColumnDto columnDto = new AvailableUsageColumnDto();
    BeanUtils.copyProperties(column, columnDto);
    List<String> sources = column.getSources().isEmpty() ? emptyList() :
        Arrays.asList(column.getSources().split("\\|"));
    columnDto.setSources(sources);
    columnDto.setSection(AvailableUsageColumnSectionDto
        .newInstance(column.getAvailableUsageColumnSection()));
    return columnDto;
  }

}
