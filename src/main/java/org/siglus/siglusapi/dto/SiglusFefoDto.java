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
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiglusFefoDto {

  private UUID orderableId;
  private UUID lotId;
  private LocalDate expirationDate;
  private Long stockOnHand;
  private Long quantityShipped;
  private Long quantitySuggested;
  private Long quantityActualFefo;

  public static SiglusFefoDto from(ShipmentLineItemDto lineItemDto, Map<UUID, LocalDate> lotIdToExpiredDateMap) {

    return SiglusFefoDto.builder()
        .orderableId(lineItemDto.getOrderable().getId())
        .lotId(lineItemDto.getLot().getId())
        .expirationDate(lotIdToExpiredDateMap.get(lineItemDto.getLot().getId()))
        .stockOnHand(lineItemDto.getStockOnHand())
        .quantityShipped(lineItemDto.getQuantityShipped())
        .build();
  }
}

