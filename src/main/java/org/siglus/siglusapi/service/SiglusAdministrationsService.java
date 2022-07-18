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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusAdministrationsService {

  @Autowired
  private AppInfoRepository appInfoRepository;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Autowired
  private FacilityExtensionRepository facilityExtensionRepository;

  public Page<FacilitySearchResultDto> searchForFacilities(FacilitySearchParamDto facilitySearchParamDto,
      Pageable pageable) {
    Page<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.searchAllFacilities(facilitySearchParamDto,
        pageable);

    List<FacilityDto> facilityDtoList = facilityDtos.getContent();
    List<FacilitySearchResultDto> facilitySearchResultDtoList = FacilitySearchResultDto.from(facilityDtoList);

    facilitySearchResultDtoList.forEach(eachFacility -> {
      FacilityExtension byFacilityId = facilityExtensionRepository.findByFacilityId(eachFacility.getId());
      eachFacility.setIsAndroidDevice(null != byFacilityId && BooleanUtils.isTrue(byFacilityId.getIsAndroid()));
    });

    return Pagination.getPage(facilitySearchResultDtoList, pageable, facilityDtos.getTotalElements());
  }

  public void eraseDeviceInfoByFacilityId(String facilityCode) {
    AppInfo androidInfoByFacilityId = appInfoRepository.findByFacilityCode(facilityCode);
    if (null == androidInfoByFacilityId) {
      log.info("The facilityCode: {} is not exist", facilityCode);
      throw new IllegalArgumentException("The facilityCode is not acceptable");
    }
    log.info("The Android device info has been removed with facilityCode: {}", facilityCode);
    appInfoRepository.deleteByFacilityCode(facilityCode);
  }
}
