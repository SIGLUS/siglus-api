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

package org.siglus.siglusapi.service.android;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;
import org.siglus.siglusapi.dto.response.android.ProductSyncResponse;
import org.siglus.siglusapi.dto.response.android.ProgramResponse;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusMeService {

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private SiglusArchiveProductService siglusArchiveProductService;

  public ProductSyncResponse getFacilityProducts(Instant lastSyncTime) {
    return new ProductSyncResponse();
  }

  public FacilityResponse getFacility() {
    UserDto userDto = authenticationHelper.getCurrentUser();
    UUID homeFacilityId = userDto.getHomeFacilityId();
    FacilityDto facilityDto = facilityReferenceDataService.getFacilityById(homeFacilityId);
    List<SupportedProgramDto> programs = facilityDto.getSupportedPrograms();
    List<ProgramResponse> programResponses = programs.stream().map(program ->
        ProgramResponse.builder()
            .code(program.getCode())
            .name(program.getName())
            .supportActive(program.isSupportActive())
            .supportStartDate(program.getSupportStartDate())
            .build()
    ).collect(Collectors.toList());
    return FacilityResponse.builder()
        .code(facilityDto.getCode())
        .name(facilityDto.getName())
        .supportedPrograms(programResponses)
        .build();
  }

  public void archiveAllProducts(List<String> productCodes) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    siglusArchiveProductService.archiveAllProducts(facilityId, productCodes);
  }
}
