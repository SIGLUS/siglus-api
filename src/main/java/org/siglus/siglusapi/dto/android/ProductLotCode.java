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
import lombok.Data;
import org.siglus.common.constant.KitConstants;

@Data
public class ProductLotCode {

  private final String productCode;

  @Nullable
  private final String lotCode;

  private ProductLotCode(String productCode, @Nullable String lotCode) {
    this.productCode = productCode;
    this.lotCode = lotCode;
  }

  public static ProductLotCode of(String productCode, String lotCode) {
    return new ProductLotCode(productCode, lotCode);
  }

  public static ProductLotCode noLot(String productCode) {
    return new ProductLotCode(productCode, null);
  }

  public boolean isNoStock() {
    return lotCode == null && !KitConstants.isKit(productCode);
  }

  public boolean isLot() {
    return lotCode != null;
  }

}
