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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductLotStock {

  private final ProductLotCode code;
  private final String productName;
  private final Lot lot;
  private final InventoryDetail inventoryDetail;

  @Builder
  private static ProductLotStock of(ProductLotCode code, String productName, java.sql.Date expirationDate,
      Integer stockQuantity, EventTime eventTime) {
    Lot lot = Lot.fromDatabase(code.getLotCode(), expirationDate);
    return new ProductLotStock(code, productName, lot, InventoryDetail.of(stockQuantity, eventTime));
  }

}
