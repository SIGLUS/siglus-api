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

package org.siglus.siglusapi.domain;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FacilityType {
  CS("CS", FacilityLevel.SITE),
  DDM("DDM", FacilityLevel.DISTRICT),
  DPM("DPM", FacilityLevel.PROVINCE),
  Central("Central", FacilityLevel.NATIONAL);


  private final String facilityType;
  private final FacilityLevel facilityLevel;


  public static Optional<FacilityType> findLevelByTypeCode(String type) {
    return Arrays.stream(values())
        .filter(e -> e.facilityType.equals(type))
        .findFirst();
  }

  public static Optional<FacilityType> findMetabaseRequestParamKeyByLevel(String level) {
    return Arrays.stream(values())
        .filter(e -> e.facilityLevel.getFacilityLevelName().equals(level))
        .findFirst();
  }

}