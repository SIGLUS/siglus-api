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

import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductLotMovement implements EventTimeContainer {

  private final ProductLotCode code;

  private final Lot lot;

  private final EventTime eventTime;

  private final MovementDetail movementDetail;

  @Nullable
  private final Integer requestedQuantity;

  private final String signature;

  @Nullable
  private final String documentNumber;

  public ProductMovementKey getProductMovementKey() {
    return ProductMovementKey.of(code.getProductCode(), eventTime);
  }

}
