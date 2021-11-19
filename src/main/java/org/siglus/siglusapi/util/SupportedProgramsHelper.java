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

package org.siglus.siglusapi.util;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedProgramsHelper {

  private final SiglusAuthenticationHelper authenticationHelper;
  private final SiglusDateHelper dateHelper;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  public Set<UUID> findHomeFacilitySupportedProgramIds() {
    return findHomeFacilitySupportedPrograms().stream()
        .map(SupportedProgramDto::getId)
        .collect(Collectors.toSet());
  }

  public List<SupportedProgramDto> findHomeFacilitySupportedPrograms() {
    FacilityDto homeFacility = getFacilityDto();
    if (homeFacility == null) {
      return Collections.emptyList();
    }
    return homeFacility.getSupportedPrograms()
        .stream()
        .filter(supportedProgramDto -> {
          LocalDate supportStartDate = supportedProgramDto.getSupportStartDate();
          return supportedProgramDto.isProgramActive()
              && supportedProgramDto.isSupportActive()
              && supportStartDate.isBefore(dateHelper.getCurrentDate());
        })
        .collect(Collectors.toList());
  }

  private FacilityDto getFacilityDto() {
    UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    return homeFacilityId == null ? null : facilityReferenceDataService.findOne(homeFacilityId);
  }

}
