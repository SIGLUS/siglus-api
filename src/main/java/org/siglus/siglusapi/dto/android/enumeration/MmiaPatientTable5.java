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

import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.DISPENSED_DS5;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_4;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmiaPatientTable5 {
  DS5(DISPENSED_DS5, NEW_COLUMN),
  DS4(DISPENSED_DS4, NEW_COLUMN_0),
  DS3(DISPENSED_DS3, NEW_COLUMN_1),
  DS2(DISPENSED_DS2, NEW_COLUMN_2),
  DS1(DISPENSED_DS1, NEW_COLUMN_3),
  DS(DISPENSED_DS, NEW_COLUMN_4);

  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(MmiaPatientTable5::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(MmiaPatientTable5::getValue)
        .findFirst().orElse(null);
  }
}
