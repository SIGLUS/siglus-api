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

package org.siglus.siglusapi.dto.android.enumeration;

import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.TABLE_PATIENTS_0TO4_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.TABLE_PATIENTS_10TO14_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.TABLE_PATIENTS_5TO9_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.MmiaPatientLineItems.TABLE_PATIENTS_ADULTS_KEY;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmiaPatientTable2 {
  PATIENTS_ADULTS_KEY(TABLE_PATIENTS_ADULTS_KEY, NEW_COLUMN),
  PATIENTS_0TO4_KEY(TABLE_PATIENTS_0TO4_KEY, NEW_COLUMN_0),
  PATIENTS_5TO9_KEY(TABLE_PATIENTS_5TO9_KEY, NEW_COLUMN_1),
  PATIENTS_10TO14_KEY(TABLE_PATIENTS_10TO14_KEY, NEW_COLUMN_2);

  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(MmiaPatientTable2::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(MmiaPatientTable2::getValue)
        .findFirst().orElse(null);
  }
}
