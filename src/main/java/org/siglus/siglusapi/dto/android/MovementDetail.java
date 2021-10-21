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
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;

@Slf4j
@ToString
@Getter
public class MovementDetail {

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
  private static MovementDetail of(Integer unsignedAdjustment, String sourceName, String sourceFacilityName,
      String destinationName, String destinationFacilityName, String adjustmentReason, String adjustmentReasonType,
      Integer unsignedInventoryAdjustment, String inventoryReason, String inventoryReasonType,
      boolean isInitInventory) {
    if (sourceName != null || sourceFacilityName != null) {
      return receive(unsignedAdjustment, sourceName != null ? sourceName : sourceFacilityName);
    } else if (destinationName != null || destinationFacilityName != null) {
      return issue(-unsignedAdjustment, destinationName != null ? destinationName : destinationFacilityName);
    } else if (adjustmentReason != null) {
      return adjustment(unsignedAdjustment, adjustmentReason, adjustmentReasonType);
    } else if (inventoryReason != null) {
      return inventory(getDirection(inventoryReasonType) * unsignedInventoryAdjustment, inventoryReason);
    } else {
      int adjustment;
      if (isInitInventory) {
        adjustment = unsignedAdjustment;
      } else {
        adjustment = 0;
      }
      return inventory(adjustment, "INVENTORY");
    }
  }

  public MovementDetail merge(MovementDetail movementDetail, UUID facilityId) {
    if (type != movementDetail.getType()) {
      String msg = String.format("Can't merge different types[@%s, %s->%s]", facilityId, type, movementDetail.type);
      throw new IllegalStateException(msg);
    }
    int mergedAdjustment = adjustment + movementDetail.getAdjustment();
    boolean sameReason = Objects.equals(this.reason, movementDetail.getReason());
    String mergedReason = sameReason ? reason : mergeReason(movementDetail, mergedAdjustment);
    return new MovementDetail(mergedAdjustment, type, mergedReason);
  }

  private static MovementDetail receive(Integer adjustment, String reason) {
    return new MovementDetail(adjustment, MovementType.RECEIVE, reason);
  }

  private static MovementDetail issue(Integer adjustment, String reason) {
    return new MovementDetail(adjustment, MovementType.ISSUE, reason);
  }

  private static MovementDetail adjustment(int unsignedAdjustment, String reason, String reasonType) {
    MovementType movementType = MovementType.ADJUSTMENT;
    int adjustment;
    if ("Unpack Kit".equals(reason)) {
      movementType = MovementType.UNPACK_KIT;
      adjustment = -unsignedAdjustment;
      reason = null;
    } else {
      adjustment = getDirection(reasonType) * unsignedAdjustment;
    }
    return new MovementDetail(adjustment, movementType, reason);
  }

  private static MovementDetail inventory(int adjustment, String reason) {
    return new MovementDetail(adjustment, MovementType.PHYSICAL_INVENTORY, reason);
  }

  private static Integer getDirection(String type) {
    if ("DEBIT".equals(type)) {
      return -1;
    } else {
      return 1;
    }
  }

  private String mergeReason(MovementDetail movementDetail, int mergedAdjustment) {
    if (mergedAdjustment < 0 && movementDetail.getAdjustment() < 0) {
      return movementDetail.reason;
    }
    return reason;
  }

}
