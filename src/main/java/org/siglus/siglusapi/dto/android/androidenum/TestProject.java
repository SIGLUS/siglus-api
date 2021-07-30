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

package org.siglus.siglusapi.dto.android.androidenum;

import static org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems.PROJECT_HIVDETERMINE;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TestProject {
  HIVDETERMINE(PROJECT_HIVDETERMINE),
  HIVUNIGOLD(NEW_COLUMN_0),
  SYPHILLIS(NEW_COLUMN_1),
  MALARIA(NEW_COLUMN_2);

  private final String value;

  public static String findByValue(String value) {
    return Arrays.stream(values())
        .filter(e -> e.value.equals(value))
        .map(Enum::name)
        .findFirst().orElse(null);
  }
}
