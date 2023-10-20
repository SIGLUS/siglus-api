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

import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_SECTION_0;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_SECTION_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.PATIENT_TYPE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_CONSUMPTION_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_NEW_PATIENT_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_PROPHYLACTICS_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_PROPHYLAXIS_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TREATMENT_ADULT_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TREATMENT_PEDIATRIC_KEY;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmtbPatientTableKeyValue {
  TREATMENT_ADULT_KEY(TABLE_TREATMENT_ADULT_KEY, NEW_SECTION_3),
  TREATMENT_PEDIATRIC_KEY(TABLE_TREATMENT_PEDIATRIC_KEY, NEW_SECTION_4),
  CONSUMPTION_KEY(TABLE_CONSUMPTION_KEY, NEW_SECTION_2),
  NEW_PATIENT_KEY(TABLE_NEW_PATIENT_KEY, PATIENT_TYPE),
  PROPHYLAXIS_KEY(TABLE_PROPHYLAXIS_KEY, NEW_SECTION_0),
  PROPHYLACTICS_KEY(TABLE_PROPHYLACTICS_KEY, NEW_SECTION_1);

  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
      .filter(e -> e.value.equals(value))
      .map(MmtbPatientTableKeyValue::getKey)
      .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
      .filter(e -> e.key.equals(key))
      .map(MmtbPatientTableKeyValue::getValue)
      .findFirst().orElse(null);
  }
}
