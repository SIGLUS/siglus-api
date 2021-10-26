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

package org.siglus.siglusapi.service.android.context;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RequiredArgsConstructor
@Getter
public class CurrentUserContext implements Context {

  private final UserDto currentUser;
  private final FacilityDto homeFacility;

  public static CurrentUserContext init(SiglusAuthenticationHelper authHelper,
      SiglusFacilityReferenceDataService facilityClient) {
    UserDto currentUser = authHelper.getCurrentUser();
    FacilityDto homeFacility = facilityClient.findOne(currentUser.getHomeFacilityId());
    return new CurrentUserContext(currentUser, homeFacility);
  }

  public List<SupportedProgramDto> getHomeFacilitySupportedPrograms() {
    return homeFacility.getSupportedPrograms().stream().filter(filterSupportedProgram()).collect(toList());
  }

  private Predicate<SupportedProgramDto> filterSupportedProgram() {
    LocalDate now = LocalDate.now();
    return supportedProgramDto -> {
      LocalDate supportStartDate = supportedProgramDto.getSupportStartDate();
      return supportedProgramDto.isProgramActive()
          && supportedProgramDto.isSupportActive()
          && supportStartDate.isBefore(now);
    };
  }

}
