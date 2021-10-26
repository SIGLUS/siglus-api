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

package org.siglus.siglusapi.exception;

import lombok.Getter;
import org.siglus.siglusapi.dto.Message;

public class UnsupportedProductsException extends BaseMessageException {

  @Getter
  private final String[] productCodes;
  private final boolean isAndroidException;

  private UnsupportedProductsException(String[] productCodes, boolean isAndroidException) {
    super(new Message("siglusapi.error.android.sync.unsupportedProduct", String.join(",", productCodes)));
    this.productCodes = productCodes;
    this.isAndroidException = isAndroidException;
  }

  public static UnsupportedProductsException asNormalException(String... productCodes) {
    return new UnsupportedProductsException(productCodes, false);
  }

  public static UnsupportedProductsException asAndroidException(String... productCodes) {
    return new UnsupportedProductsException(productCodes, true);
  }

  @Override
  public boolean isAndroidException() {
    return isAndroidException;
  }

}