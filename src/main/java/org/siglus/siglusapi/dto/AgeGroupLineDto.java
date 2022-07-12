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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openlmis.requisition.dto.BaseDto;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupLineDto extends BaseDto {

  private UUID requisitionId;

  private String groupName;

  private String columnName;

  private Integer value;

  public static List<AgeGroupLineDto> from(List<AgeGroupLineItem> ageGroupLineItemList) {
    LinkedList<AgeGroupLineDto> ageGroupLineDtos = new LinkedList<>();
    ageGroupLineItemList.forEach(ageGroupLineItem -> {
      AgeGroupLineDto ageGroupLineDto = new AgeGroupLineDto();
      BeanUtils.copyProperties(ageGroupLineItem, ageGroupLineDto);
      ageGroupLineDtos.add(ageGroupLineDto);
    });
    return ageGroupLineDtos;
  }

}
