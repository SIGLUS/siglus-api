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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductMovement implements EventTimeContainer {

  private final UUID id;
  private final String productCode;
  private final EventTime eventTime;
  private final Integer requestedQuantity;
  private final MovementDetail movementDetail;
  private final Integer stockQuantity;
  private final String signature;
  private final String documentNumber;
  private final String sourcefreetext;
  private final String destinationfreetext;
  @Nullable
  private final Instant processedAt;
  @Default
  private final List<LotMovement> lotMovements = Collections.emptyList();

  public ProductMovementKey getProductMovementKey() {
    return ProductMovementKey.of(productCode, eventTime);
  }

  public Integer getInventoryBeforeAdjustment() {
    return stockQuantity - movementDetail.getAdjustment();
  }

  public boolean isRightAfter(ProductMovement previous) {
    return getInventoryBeforeAdjustment().equals(previous.stockQuantity);
  }

}
