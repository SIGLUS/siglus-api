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
import lombok.Getter;
import lombok.Setter;

@Getter
public class StockCard {

  @Setter
  private UUID id;
  private final UUID facilityId;
  private final UUID programId;
  private final UUID productId;
  private final String productCode;
  private final UUID lotId;
  private final String lotCode;
  private final LocalDate expirationDate;

  private StockCard(UUID facilityId, UUID programId, UUID productId, String productCode, UUID lotId, String lotCode,
      LocalDate expirationDate) {
    this.facilityId = facilityId;
    this.programId = programId;
    this.productId = productId;
    this.productCode = productCode;
    this.lotId = lotId;
    this.lotCode = lotCode;
    this.expirationDate = expirationDate;
  }

  public static StockCard of(UUID facilityId, UUID programId, UUID productId, String productCode, UUID lotId,
      String lotCode, LocalDate expirationDate) {
    return new StockCard(facilityId, programId, productId, productCode, lotId, lotCode, expirationDate);
  }

  public static StockCard ofNoLot(UUID facilityId, UUID programId, UUID productId, String productCode) {
    return new StockCard(facilityId, programId, productId, productCode, null, null, null);
  }

}
