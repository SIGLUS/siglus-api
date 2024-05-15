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

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FacilityConfigHelper {

  private final SiglusAuthenticationHelper authHelper;
  private final FacilityExtensionRepository facilityExtensionRepository;

  public boolean isStockManagement() {
    UserDto currentUser = authHelper.getCurrentUser();
    if (currentUser == null) {
      // trusted-client
      return false;
    }
    return !isLocationManagementEnabled(currentUser.getHomeFacilityId());
  }


  public boolean isLocationManagement() {
    UserDto currentUser = authHelper.getCurrentUser();
    if (currentUser == null) {
      // trusted-client
      return false;
    }
    return isLocationManagementEnabled(currentUser.getHomeFacilityId());
  }

  public boolean isLocationManagementEnabled(UUID facilityId) {
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (facilityExtension == null || facilityExtension.getEnableLocationManagement() == null) {
      return false;
    }
    return facilityExtension.getEnableLocationManagement();
  }
}
