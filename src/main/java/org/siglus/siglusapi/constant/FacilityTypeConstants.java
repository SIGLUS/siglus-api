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

package org.siglus.siglusapi.constant;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FacilityTypeConstants {

  public static final String CS = "CS";

  public static final String PS = "PS";

  public static final String HG = "HG";

  public static final String HP = "HP";

  public static final String HR = "HR";

  public static final String HD = "HD";

  public static final String OUTROS = "OUTROS";

  public static final String HPSIQ = "HPSIQ";

  public static final String HM = "HM";

  public static final String DDM = "DDM";

  public static final String DPM = "DPM";

  public static final String HC = "HC";

  public static final String AI = "AI";

  public static final String AC = "AC";

  public static final String CENTRAL = "Central";

  public static Set<String> getAndroidOriginMovementTypes() {
    return Stream.of(DPM, DDM, AI).collect(Collectors.toSet());
  }

  public static Set<String> getVirtualFacilityTypes() {
    return Stream.of(CENTRAL, AC).collect(Collectors.toSet());
  }

}
