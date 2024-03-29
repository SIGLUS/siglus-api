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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplifyOrderablesDto extends AvailableOrderablesDto {

  private Boolean archived;

  public static SimplifyOrderablesDto from(OrderableDto orderableDto) {
    return SimplifyOrderablesDto.builder()
        .orderableId(orderableDto.getId())
        .fullProductName(orderableDto.getFullProductName())
        .productCode(orderableDto.getProductCode())
        .programId(null != orderableDto.getPrograms()
            ? orderableDto.getPrograms().stream().findFirst().map(ProgramOrderableDto::getProgramId).orElse(null)
            : null)
        .isKit(orderableDto.getIsKit())
        .dispensable(orderableDto.getDispensable())
        .archived(orderableDto.getArchived())
        .build();
  }
}
