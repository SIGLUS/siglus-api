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

package org.openlmis.fulfillment.domain;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public enum OrderStatus {
  ORDERED,
  FULFILLING,
  SHIPPED,
  RECEIVED,
  TRANSFER_FAILED,
  IN_ROUTE,
  READY_TO_PACK;

  /**
   * Find a correct {@link OrderStatus} instance based on the passed string. The method ignores
   * the case.
   *
   * @param arg string representation of one of order status.
   * @return instance of {@link OrderStatus} if the given string matches status; otherwise null.
   */
  public static OrderStatus fromString(String arg) {
    for (OrderStatus status : values()) {
      if (equalsIgnoreCase(arg, status.name())) {
        return status;
      }
    }

    return null;
  }
}
