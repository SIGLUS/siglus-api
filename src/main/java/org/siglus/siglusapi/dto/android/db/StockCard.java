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

import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StockCard {

  private final UUID id;
  private final UUID facilityId;
  private final UUID programId;
  private final UUID productId;
  private final String productCode;
  private final UUID lotId;
  private final String lotCode;
  private final LocalDate expirationDate;
  private final UUID stockEventId;

  public static StockCard querySample(UUID facilityId, UUID programId, UUID productId, ProductLot productLot) {
    LocalDate expirationDate = productLot.getLot() == null ? null : productLot.getLot().getExpirationDate();
    return new StockCard(null, facilityId, programId, productId, productLot.getProductCode(),
        productLot.getId(), productLot.getLotCode(), expirationDate, null);
  }

  public static StockCard of(UUID facilityId, UUID programId, UUID productId, ProductLot productLot,
      StockEvent stockEvent) {
    LocalDate expirationDate = productLot.getLot() == null ? null : productLot.getLot().getExpirationDate();
    return new StockCard(UUID.randomUUID(), facilityId, programId, productId, productLot.getProductCode(),
        productLot.getId(), productLot.getLotCode(), expirationDate, stockEvent.getId());
  }

  public static StockCard fromDatabase(UUID id, StockCard querySample) {
    return new StockCard(id, querySample.facilityId, querySample.programId, querySample.productId,
        querySample.productCode, querySample.lotId, querySample.lotCode, querySample.expirationDate,
        querySample.stockEventId);
  }

}
