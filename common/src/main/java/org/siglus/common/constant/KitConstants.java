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

package org.siglus.common.constant;

import java.util.Arrays;
import java.util.List;

public class KitConstants {

  private KitConstants() {
  }

  public static final String KIT_26A01 = "26A01";
  public static final String KIT_26A02 = "26A02";
  public static final String KIT_26B01 = "26B01";
  public static final String KIT_26B02 = "26B02";

  public static final List<String> US_KITS = Arrays.asList(KIT_26A01, KIT_26B01);
  public static final List<String> APE_KITS = Arrays.asList(KIT_26A02, KIT_26B02);
  public static final List<String> ALL_KITS = Arrays.asList(KIT_26A01, KIT_26A02, KIT_26B01, KIT_26B02);

  public static boolean isKit(String productCode) {
    if (productCode == null) {
      return false;
    }
    return ALL_KITS.stream().anyMatch(productCode::equals);
  }

}
