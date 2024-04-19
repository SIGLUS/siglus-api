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

import static org.siglus.siglusapi.constant.FieldConstants.ALL_PROGRAM;

import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SiglusPhysicalInventoryHistoryListDto {

  private UUID groupId;
  private String programName;
  private LocalDate completedDate;
  private String lineItemData;

  public static SiglusPhysicalInventoryHistoryListDto from(Entry<UUID, List<SiglusPhysicalInventoryHistoryDto>> entry) {
    return SiglusPhysicalInventoryHistoryListDto.builder()
        .groupId(entry.getKey())
        .programName(entry.getValue().size() > 1 ? ALL_PROGRAM : entry.getValue().get(0).getProgramName())
        .completedDate(entry.getValue().get(0).getCompletedDate())
        .lineItemData(entry.getValue().get(0).getLineItemData())
        .build();
  }
}
