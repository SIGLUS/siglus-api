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

import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.KEY_REGIME_3LINES_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.KEY_REGIME_3LINES_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.KEY_REGIME_3LINES_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.STLINHAS;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RegimenSummaryCode {
  REGIME_3LINES_1(KEY_REGIME_3LINES_1, STLINHAS),
  REGIME_3LINES_2(KEY_REGIME_3LINES_2, NEW_COLUMN_0),
  REGIME_3LINES_3(KEY_REGIME_3LINES_3, NEW_COLUMN_1);

  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(RegimenSummaryCode::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(RegimenSummaryCode::getValue)
        .findFirst().orElse(null);
  }
}
