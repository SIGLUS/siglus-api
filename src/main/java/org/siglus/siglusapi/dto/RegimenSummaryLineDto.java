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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegimenSummaryLineDto {

  private String name;

  // column: value
  private Map<String, RegimenColumnDto> columns;

  public static List<RegimenSummaryLineDto> from(List<RegimenSummaryLineItem> lineItems) {
    List<RegimenSummaryLineDto> regimenSummaryLineDtos = newArrayList();

    Map<String, List<RegimenSummaryLineItem>> groupBySummaryRow = lineItems.stream()
        .collect(Collectors.groupingBy(RegimenSummaryLineItem::getName));

    groupBySummaryRow.forEach((rowName, regimenSummaryLineItems) -> {

      Map<String, RegimenColumnDto> columnMap = regimenSummaryLineItems.stream()
          .collect(Collectors.toMap(RegimenSummaryLineItem::getColumn,
              regimenSummaryLineItem -> RegimenColumnDto.builder()
                  .id(regimenSummaryLineItem.getId())
                  .value(regimenSummaryLineItem.getValue())
                  .build()));

      RegimenSummaryLineDto lineDto = new RegimenSummaryLineDto();
      lineDto.setColumns(columnMap);
      lineDto.setName(rowName);

      regimenSummaryLineDtos.add(lineDto);
    });

    return regimenSummaryLineDtos;

  }

}
