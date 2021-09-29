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

import org.openlmis.fulfillment.util.Message;

public class ProductNotSupportException extends RuntimeException {

  private final String messageKey;
  private final String[] productCodes;

  public ProductNotSupportException(String messageKey, String... productCodes) {
    super(messageKey);
    this.messageKey = messageKey;
    this.productCodes = productCodes;
  }

  public ProductNotSupportException(Throwable cause, String messageKey, String...productCodes) {
    super(messageKey, cause);
    this.messageKey = messageKey;
    this.productCodes = productCodes;
  }

  public Message asMessage() {
    return new Message(this.messageKey, (Object[]) this.productCodes);
  }

  @Override
  public String getMessage() {
    return this.asMessage().toString();
  }

  public String getMessageKey() {
    return this.messageKey;
  }

  public String[] getProductCodes() {
    return this.productCodes;
  }
}