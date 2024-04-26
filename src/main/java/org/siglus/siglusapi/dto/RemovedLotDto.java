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

import java.time.LocalDate;
import java.util.UUID;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.index.qual.Positive;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemovedLotDto {
  @NotNull
  private UUID stockCardId;
  @NotNull
  private UUID facilityId;
  @NotNull
  private UUID programId;
  @NotNull
  private UUID orderableId;
  @NotNull
  private UUID lotId;
  @Positive
  private int quantity;
  private String locationCode;
  private String area;

  public StockEventLineItemDto toStockEventLineItemDto(UUID reasonId) {
    return StockEventLineItemDto.builder()
            .reasonId(reasonId)
            .destinationId(facilityId)
            .orderableId(orderableId)
            .lotId(lotId)
            .area(area)
            .quantity(quantity)
            .locationCode(locationCode)
            .occurredDate(LocalDate.now())
            .build();
  }
}
