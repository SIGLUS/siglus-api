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

package org.siglus.siglusapi.dto.android.db;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PhysicalInventoryLineDetail {

  private final UUID physicalInventoryId;
  private final UUID productId;
  private final UUID lotId;
  private final UUID reasonId;
  private final Integer adjustment;
  private final Integer inventoryBeforeAdjustment;

  public static PhysicalInventoryLineDetail of(PhysicalInventory physicalInventory, StockCard stockCard,
      MovementType type, StockCardAdjustment request) {
    Integer adjustment = request.getQuantity();
    Integer inventoryBeforeAdjustment = request.getStockOnHand() - adjustment;
    UUID programId = stockCard.getProgramId();
    String reason = request.getReasonName();
    UUID reasonId = type.getInventoryReasonId(programId, reason);
    return new PhysicalInventoryLineDetail(physicalInventory.getId(), stockCard.getProductId(), stockCard.getLotId(),
        reasonId, adjustment, inventoryBeforeAdjustment);
  }

}
