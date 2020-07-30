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
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.RegimenLineItem;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegimenLineDto {

  private RegimenDto regimen;

  // column: value
  private Map<String, RegimenColumnDto> columns;

  public static List<RegimenLineDto> from(List<RegimenLineItem> lineItems,
      Map<UUID, RegimenDto> regimenDtoMap) {
    List<RegimenLineDto> regimenLineDtos = newArrayList();

    Map<UUID, List<RegimenLineItem>> groupByRegimen =
        lineItems.stream().collect(Collectors.groupingBy(RegimenLineItem::getRegimenId));

    groupByRegimen.forEach((regimenId, regimenLineItems) -> {

      Map<String, RegimenColumnDto> columnMap = regimenLineItems.stream()
          .collect(Collectors.toMap(RegimenLineItem::getColumn,
              regimenLineItem -> RegimenColumnDto.builder()
                  .id(regimenId)
                  .value(regimenLineItem.getValue())
                  .build()));

      RegimenLineDto lineDto = new RegimenLineDto();
      lineDto.setColumns(columnMap);
      lineDto.setRegimen(regimenDtoMap.get(regimenId));

      regimenLineDtos.add(lineDto);
    });

    return regimenLineDtos;

  }
}
