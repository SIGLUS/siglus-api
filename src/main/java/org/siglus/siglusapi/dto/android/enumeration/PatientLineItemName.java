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

import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_8;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.PATIENT_TYPE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_ARVT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DM_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PATIENTS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PROPHYLAXY_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_TOTAL_KEY;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PatientLineItemName {
  ARVT_KEY(TABLE_ARVT_KEY, PATIENT_TYPE),
  PATIENTS_KEY(TABLE_PATIENTS_KEY, NEW_SECTION_0),
  PROPHYLAXY_KEY(TABLE_PROPHYLAXY_KEY, NEW_SECTION_1),
  DISPENSED_DS_KEY(TABLE_DISPENSED_DS_KEY, NEW_SECTION_2),
  DISPENSED_DT_KEY(TABLE_DISPENSED_DT_KEY, NEW_SECTION_3),
  DISPENSED_DM_KEY(TABLE_DISPENSED_DM_KEY, NEW_SECTION_4),
  TOTAL_KEY(TABLE_TOTAL_KEY, NEW_SECTION_8);

  private final String key;

  private final String value;

  public static String findKeyByValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(PatientLineItemName::getKey)
        .findFirst().orElse(null);
  }

  public static String findValueByKey(String key) {
    if (key == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(e -> e.key.equals(key))
        .map(PatientLineItemName::getValue)
        .findFirst().orElse(null);
  }
}
