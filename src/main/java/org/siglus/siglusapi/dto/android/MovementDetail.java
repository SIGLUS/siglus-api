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

package org.siglus.siglusapi.dto.android;

import java.util.Objects;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;

@EqualsAndHashCode
@ToString
@Getter
public class MovementDetail {

  private static final String INVENTORY_NOT_POPULATED = "Inventory not populated";

  private final Integer adjustment;
  private final MovementType type;
  @Nullable
  private final String reason;

  public MovementDetail(Integer adjustment, MovementType type, @Nullable String reason) {
    this.adjustment = adjustment;
    this.type = type;
    this.reason = reason;
  }

  @SuppressWarnings("java:S107")
  @Builder(access = AccessLevel.PUBLIC)
  private static MovementDetail of(Integer unsignedAdjustment, String source, String destination,
      String adjustmentReason, String adjustmentReasonType,
      Integer unsignedInventoryAdjustment, String inventoryReason, String inventoryReasonType) {
    MovementType movementType;
    Integer adjustment;
    String reasonName;
    if (source != null) {
      movementType = MovementType.RECEIVE;
      adjustment = unsignedAdjustment;
      reasonName = movementType.getReason(source, adjustment);
    } else if (destination != null) {
      movementType = MovementType.ISSUE;
      adjustment = -unsignedAdjustment;
      reasonName = movementType.getReason(destination, adjustment);
    } else if (adjustmentReason != null) {
      if ("Unpack Kit".equals(adjustmentReason)) {
        movementType = MovementType.UNPACK_KIT;
        adjustment = -unsignedAdjustment;
        reasonName = null;
      } else {
        movementType = MovementType.ADJUSTMENT;
        if ("DEBIT".equals(adjustmentReasonType)) {
          adjustment = -unsignedAdjustment;
        } else {
          adjustment = unsignedAdjustment;
        }
        reasonName = movementType.getReason(adjustmentReason, adjustment);
      }
    } else if (inventoryReason != null) {
      movementType = MovementType.PHYSICAL_INVENTORY;
      if ("DEBIT".equals(inventoryReasonType)) {
        adjustment = -unsignedInventoryAdjustment;
      } else {
        adjustment = unsignedInventoryAdjustment;
      }
      reasonName = movementType.getReason(inventoryReason, adjustment);
    } else {
      movementType = MovementType.PHYSICAL_INVENTORY;
      adjustment = 0;
      reasonName = "INVENTORY";
    }
    return new MovementDetail(adjustment, movementType, reasonName);
  }

  public MovementDetail merge(MovementDetail movementDetail) {
    if (type != movementDetail.getType()) {
      throw new IllegalStateException("Can't merge different types");
    }
    int mergedAdjustment = adjustment + movementDetail.getAdjustment();
    boolean differentReason = !Objects.equals(this.reason, movementDetail.getReason());
    String mergedReason = differentReason ? null : reason;
    return new MovementDetail(mergedAdjustment, type, mergedReason);
  }

}
