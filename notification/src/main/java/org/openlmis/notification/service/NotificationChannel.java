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

package org.openlmis.notification.service;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public enum NotificationChannel {
  EMAIL, SMS;

  /**
   * Tries to convert the given string value to one of available {@link NotificationChannel}.
   *
   * @param value a string representation of one of {@link NotificationChannel}.
   * @return related {@link NotificationChannel} or null.
   */
  public static NotificationChannel fromString(String value) {
    return Arrays
        .stream(values())
        .filter(candidate -> StringUtils.equalsIgnoreCase(candidate.name(), value))
        .findFirst()
        .orElse(null);
  }
}
