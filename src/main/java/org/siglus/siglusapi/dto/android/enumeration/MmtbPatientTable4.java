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

import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_ADULT_MR_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_ADULT_SENSITIVE_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_ADULT_XR_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_CHILD_MR_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_CHILD_SENSITIVE_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_CHILD_XR_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_NEW_PATIENT_TOTAL_KEY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TOTAL_COLUMN;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmtbPatientTable4 {
  TB_LABEL_NEW_ADULT_SENSITIVE_KEY(TABLE_TB_LABEL_NEW_ADULT_SENSITIVE_KEY, NEW_COLUMN),
  TB_LABEL_NEW_ADULT_MR_KEY(TABLE_TB_LABEL_NEW_ADULT_MR_KEY, NEW_COLUMN_0),
  TB_LABEL_NEW_ADULT_XR_KEY(TABLE_TB_LABEL_NEW_ADULT_XR_KEY, NEW_COLUMN_1),
  TB_LABEL_NEW_CHILD_SENSITIVE_KEY(TABLE_TB_LABEL_NEW_CHILD_SENSITIVE_KEY, NEW_COLUMN_2),
  TB_LABEL_NEW_CHILD_MR_KEY(TABLE_TB_LABEL_NEW_CHILD_MR_KEY, NEW_COLUMN_3),
  TB_LABEL_NEW_CHILD_XR_KEY(TABLE_TB_LABEL_NEW_CHILD_XR_KEY, NEW_COLUMN_4),
  TB_LABEL_NEW_PATIENT_TOTAL_KEY(TABLE_TB_LABEL_NEW_PATIENT_TOTAL_KEY, TOTAL_COLUMN);
  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(MmtbPatientTable4::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(MmtbPatientTable4::getValue)
        .findFirst().orElse(null);
  }
}
