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
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;

@Getter
public class ProductLot {

  @Setter
  private UUID id;

  private final String productCode;

  private final Lot lot;

  public ProductLot(ProductLotCode code, LocalDate expirationDate) {
    this.productCode = code.getProductCode();
    if (code.isLot()) {
      this.lot = new Lot(code.getLotCode(), expirationDate);
    } else {
      this.lot = null;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ProductLot)) {
      return false;
    }
    ProductLot productLot = (ProductLot) other;
    if (productLot.getLot() == null || productLot.getLot().getCode() == null
        || productLot.getLot().getExpirationDate() == null) {
      return productCode.equals(productLot.getProductCode());
    }
    return Objects.equals(productCode, productLot.getProductCode())
        && Objects.equals(lot.getCode(), productLot.getLot().getCode())
        && Objects.equals(lot.getExpirationDate(), productLot.getLot().getExpirationDate());
  }

  @Override
  public int hashCode() {
    return Objects.hash(productCode, lot);
  }
}
