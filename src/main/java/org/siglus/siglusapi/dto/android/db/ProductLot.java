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

import static org.openlmis.referencedata.domain.Orderable.TRADE_ITEM;

import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.OrderableDto;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;

@Getter
@RequiredArgsConstructor
public class ProductLot {

  private final UUID id;
  private final String productCode;
  private final UUID tradeItemId;
  private final Lot lot;

  public static ProductLot noLot(String productCode) {
    return new ProductLot(null, productCode, null, null);
  }

  public static ProductLot fromDatabase(UUID id, String productCode, UUID tradeItemId, Lot lot) {
    return new ProductLot(id, productCode, tradeItemId, lot);
  }

  public static ProductLot of(OrderableDto product, Lot lot) {
    String tradeItemId = product.getIdentifiers().get(TRADE_ITEM);
    return new ProductLot(UUID.randomUUID(), product.getProductCode(), UUID.fromString(tradeItemId), lot);
  }

  public static ProductLot of(String productCode, UUID tradeItemId, Lot lot) {
    return new ProductLot(UUID.randomUUID(), productCode, tradeItemId, lot);
  }

  @Nullable
  public String getLotCode() {
    return lot == null ? null : lot.getCode();
  }

  @Nullable
  public LocalDate getExpirationDate() {
    return lot == null ? null : lot.getExpirationDate();
  }

  public boolean isLot() {
    return lot != null;
  }

  public ProductLotCode toProductLotCode() {
    if (isLot()) {
      return ProductLotCode.of(productCode, lot.getCode());
    }
    return ProductLotCode.noLot(productCode);
  }

}
