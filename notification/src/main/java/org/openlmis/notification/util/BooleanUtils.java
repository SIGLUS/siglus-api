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

package org.openlmis.notification.util;

public class BooleanUtils {

  private BooleanUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Tries to convert the given object value to {@link Boolean}.
   *
   * @param value any kind of object
   * @return true if object can be mapped to true value; otherwise false.
   */
  public static Boolean toBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }

    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }

    if (value instanceof Number) {
      return ((Number) value).longValue() > 0;
    }

    return false;
  }
}
