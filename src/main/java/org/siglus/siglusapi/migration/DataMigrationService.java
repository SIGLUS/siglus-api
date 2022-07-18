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

package org.siglus.siglusapi.migration;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.siglus.siglusapi.util.SiglusAuthenticationHelper.MIGRATE_DATA;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.service.android.MeService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.stereotype.Service;

@Service
public class DataMigrationService {

  private static final String NO_USER_FOUND_IN_FACILITY = "no user found in facility %s";
  private final MeService meService;
  private final SiglusUserReferenceDataService userReferenceDataService;
  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;

  public DataMigrationService(
      MeService meService,
      SiglusUserReferenceDataService userReferenceDataService,
      SiglusSimulateUserAuthHelper simulateUserAuthHelper) {
    this.meService = meService;
    this.userReferenceDataService = userReferenceDataService;
    this.simulateUserAuthHelper = simulateUserAuthHelper;
  }

  public CreateStockCardResponse createStockCards(
      String facilityId, List<StockCardCreateRequest> requests) {
    assumeOneUserInFacility(facilityId);
    return meService.createStockCards(requests);
  }

  private void assumeOneUserInFacility(String facilityId) {
    UserDto assumedUser = findOneUserInFacility(facilityId);
    simulateUserAuthHelper.simulateUserAuth(assumedUser.getId(), singleton(MIGRATE_DATA));
  }

  private UserDto findOneUserInFacility(String facilityId) {
    return userReferenceDataService.getUserInfo(UUID.fromString(facilityId)).getContent().stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException(format(NO_USER_FOUND_IN_FACILITY, facilityId)));
  }

  public void createOrUpdateCmms(String facilityId, List<HfCmmDto> hfCmmDtos) {
    assumeOneUserInFacility(facilityId);
    meService.processHfCmms(hfCmmDtos);
  }
}
