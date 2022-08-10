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

package org.siglus.siglusapi.dto.enums;

import static org.siglus.siglusapi.constant.FieldConstants.LOCATION;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum LocationManagementOption {
  BY_PRODUCT(PRODUCT),
  BY_LOCATION(LOCATION);

  private final String value;

  @Override
  public String toString() {
    return value;
  }

  public static LocationManagementOption fromString(String text) {
    for (LocationManagementOption b : LocationManagementOption.values()) {
      if (b.value.equalsIgnoreCase(text)) {
        return b;
      }
    }
    throw new IllegalArgumentException("no location management option with value " + text + " found");
  }
}
