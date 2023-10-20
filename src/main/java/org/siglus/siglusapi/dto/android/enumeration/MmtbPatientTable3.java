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
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_5;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.NEW_COLUMN_6;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_5;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_6;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_7;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmtbPatientLineItems.TABLE_TB_LABEL_CONSUMPTION_KEY_8;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmtbPatientTable3 {
  TB_LABEL_CONSUMPTION_KEY_1(TABLE_TB_LABEL_CONSUMPTION_KEY_1, NEW_COLUMN),
  TB_LABEL_CONSUMPTION_KEY_2(TABLE_TB_LABEL_CONSUMPTION_KEY_2, NEW_COLUMN_0),
  TB_LABEL_CONSUMPTION_KEY_3(TABLE_TB_LABEL_CONSUMPTION_KEY_3, NEW_COLUMN_1),
  TB_LABEL_CONSUMPTION_KEY_4(TABLE_TB_LABEL_CONSUMPTION_KEY_4, NEW_COLUMN_2),
  TB_LABEL_CONSUMPTION_KEY_5(TABLE_TB_LABEL_CONSUMPTION_KEY_5, NEW_COLUMN_3),
  TB_LABEL_CONSUMPTION_KEY_6(TABLE_TB_LABEL_CONSUMPTION_KEY_6, NEW_COLUMN_4),
  TB_LABEL_CONSUMPTION_KEY_7(TABLE_TB_LABEL_CONSUMPTION_KEY_7, NEW_COLUMN_5),
  TB_LABEL_CONSUMPTION_KEY_8(TABLE_TB_LABEL_CONSUMPTION_KEY_8, NEW_COLUMN_6);
  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(MmtbPatientTable3::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(MmtbPatientTable3::getValue)
        .findFirst().orElse(null);
  }
}
