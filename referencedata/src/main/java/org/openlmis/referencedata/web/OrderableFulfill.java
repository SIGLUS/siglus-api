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

package org.openlmis.referencedata.web;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderableFulfill {
  private final List<UUID> canFulfillForMe;
  private final List<UUID> canBeFulfilledByMe;

  public static OrderableFulfill ofTradeItem(UUID... canBeFulfilledByMe) {
    return ofTradeItem(Arrays.asList(canBeFulfilledByMe));
  }

  public static OrderableFulfill ofTradeItem(List<UUID> canBeFulfilledByMe) {
    return new OrderableFulfill(emptyList(), canBeFulfilledByMe);
  }

  public static OrderableFulfill ofCommodityType(UUID... canFulfillForMe) {
    return ofCommodityType(Arrays.asList(canFulfillForMe));
  }

  public static OrderableFulfill ofCommodityType(List<UUID> canFulfillForMe) {
    return new OrderableFulfill(canFulfillForMe, emptyList());
  }

}
