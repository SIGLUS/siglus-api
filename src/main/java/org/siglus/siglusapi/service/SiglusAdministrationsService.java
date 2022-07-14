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

package org.siglus.siglusapi.service;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusAdministrationsService {

  @Autowired
  private AppInfoRepository appInfoRepository;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  public Page<FacilityDto> searchForFacilities(Integer page, Integer size, String sort) {
    // get results from OpenLMIS interface
    Page<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.searchAllFacilities(page, size, sort);
    // justify isAndroid by facilityId
    facilityDtos.getContent().forEach(m -> {
      // FacilityExtension byFacilityId = facilityExtensionRepository.findByFacilityId(m.getId());
      // m.setIsAndroid(null != byFacilityId && BooleanUtils.isTrue(byFacilityId.getIsAndroid()));
    });
    return facilityDtos;
  }

  public void eraseAndroidByFacilityId(UUID facilityId) {
    AppInfo one = appInfoRepository.findOne(facilityId);
    if (null == one) {
      log.info("The facilityId: {} is not exist", facilityId);
      throw new IllegalArgumentException("The facilityId is not acceptable");
    }
    appInfoRepository.delete(facilityId);
  }
}
