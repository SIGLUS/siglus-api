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

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class DeviceHelper {

  private final SiglusAuthenticationHelper authHelper;
  private final AppInfoRepository appInfoRepository;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  public boolean isSameDevice() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
    HttpServletRequest request = sra.getRequest();
    String uniqueId = request.getHeader("UniqueId");
    UserDto currentUser = authHelper.getCurrentUser();
    if (currentUser == null || uniqueId == null) {
      return false;
    }
    FacilityDto facility = facilityReferenceDataService.findOne(currentUser.getHomeFacilityId());
    AppInfo appInfo = appInfoRepository.findByFacilityCode(facility.getCode());
    return appInfo == null || uniqueId.equals(appInfo.getUniqueId());
  }
}
