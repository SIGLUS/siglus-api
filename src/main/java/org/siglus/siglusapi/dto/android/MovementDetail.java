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
import org.siglus.siglusapi.service.android.SiglusMeService.MovementType;

@EqualsAndHashCode
@ToString
@Getter
public class MovementDetail {

  private final Integer adjustment;
  @Nullable
  private final Integer inventory;
  private final MovementType type;
  @Nullable
  private final String reason;

  public MovementDetail(Integer adjustment, Integer inventory, MovementType type, @Nullable String reason) {
    this.adjustment = adjustment;
    this.inventory = inventory;
    this.type = type;
    this.reason = reason;
  }

  @SuppressWarnings("java:S107")
  @Builder(access = AccessLevel.PUBLIC)
  private static MovementDetail of(Integer unsignedAdjustment, String source, String destination,
      String adjustmentReason, String adjustmentReasonType,
      Integer unsignedInventoryAdjustment, String inventoryReason, String inventoryReasonType) {
    MovementType movementType;
    if (source != null) {
      movementType = MovementType.RECEIVE;
      return new MovementDetail(unsignedAdjustment, null, movementType,
          movementType.getReason(source, unsignedAdjustment));
    }
    if (destination != null) {
      movementType = MovementType.ISSUE;
      return new MovementDetail(-1 * unsignedAdjustment, null, movementType,
          movementType.getReason(destination, -1 * unsignedAdjustment));
    }
    if (adjustmentReason != null) {
      if ("Unpack Kit".equals(adjustmentReason)) {
        return new MovementDetail(-1 * unsignedAdjustment, null, MovementType.UNPACK_KIT, null);
      }
      movementType = MovementType.ADJUSTMENT;
      Integer adjustment = unsignedAdjustment;
      if ("DEBIT".equals(adjustmentReasonType)) {
        adjustment = -1 * unsignedAdjustment;
      }
      return new MovementDetail(adjustment, null, movementType, movementType.getReason(adjustmentReason, adjustment));
    }
    if (inventoryReason != null) {
      Integer adjustment = unsignedInventoryAdjustment;
      if ("DEBIT".equals(inventoryReasonType)) {
        adjustment = -1 * unsignedInventoryAdjustment;
      }
      movementType = MovementType.PHYSICAL_INVENTORY;
      return new MovementDetail(adjustment, null, movementType, movementType.getReason(inventoryReason, adjustment));
    }
    return new MovementDetail(0, unsignedAdjustment, MovementType.PHYSICAL_INVENTORY, null);
  }

  public MovementDetail populateInventory(Integer inventory) {
    Objects.requireNonNull(inventory);
    if (this.inventory != null && !this.inventory.equals(inventory)) {
      throw new IllegalStateException("Inconsistent inventory");
    }
    return new MovementDetail(adjustment, inventory, type, reason);
  }

  public MovementDetail assemble(MovementDetail movementDetail) {
    if (type != movementDetail.getType()) {
      throw new IllegalStateException("Can't assemble different types");
    }
    if (inventory == null || movementDetail.inventory == null) {
      throw new IllegalStateException("Inventory not populated");
    }
    int assembledAdjustment = adjustment + movementDetail.getAdjustment();
    int assembledInventory = inventory + movementDetail.inventory;
    boolean differentReason = Objects.equals(this.reason, movementDetail.getReason());
    String assembledReason = differentReason ? null : reason;
    return new MovementDetail(assembledAdjustment, assembledInventory, type, assembledReason);
  }

  public Integer getInitInventory() {
    if (inventory == null) {
      throw new IllegalStateException("Inventory not populated");
    }
    return inventory - adjustment;
  }

  public boolean isRightAfter(MovementDetail former) {
    if (inventory == null) {
      throw new IllegalStateException("Inventory not populated");
    }
    return inventory - adjustment == former.adjustment;
  }

}
