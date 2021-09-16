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

package org.siglus.siglusapi.errorhandling.exception;

import org.openlmis.fulfillment.util.Message;

public class OrderNotFoundException extends RuntimeException {

  private static final String MESSAGE_KEY = "siglusapi.pod.order.notFoundByCode";
  private final String orderCode;

  public OrderNotFoundException(String orderCode) {
    super(MESSAGE_KEY);
    this.orderCode = orderCode;
  }

  public OrderNotFoundException(Throwable cause, String orderCode) {
    super(MESSAGE_KEY, cause);
    this.orderCode = orderCode;
  }

  public Message asMessage() {
    return new Message(MESSAGE_KEY, this.orderCode);
  }

  public String getMessage() {
    return this.asMessage().toString();
  }

  public String getMessageKey() {
    return MESSAGE_KEY;
  }

  public String getOrderCode() {
    return this.orderCode;
  }
}
