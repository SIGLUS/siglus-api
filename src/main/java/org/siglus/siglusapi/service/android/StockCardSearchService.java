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

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import javax.annotation.ParametersAreNullableByDefault;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class StockCardSearchService {

  private final SiglusAuthenticationHelper authHelper;

  private final StockManagementRepository stockManagementRepository;

  private final ProductMovementMapper mapper;

  @ParametersAreNullableByDefault
  public FacilityProductMovementsResponse getProductMovementsByTime(LocalDate since, LocalDate tillExclusive) {
    if (since == null) {
      since = LocalDate.now().withDayOfYear(1);
    }
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    LocalDate till = null;
    if (tillExclusive != null) {
      till = tillExclusive.minusDays(1);
    }
    PeriodOfProductMovements period = stockManagementRepository.getAllProductMovements(facilityId, since, till);
    return mapper.toResponses(period);
  }

  public FacilityProductMovementsResponse getProductMovementsByOrderables(Set<UUID> orderableIds) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    PeriodOfProductMovements period = stockManagementRepository.getAllProductMovements(facilityId, orderableIds);
    return mapper.toResponses(period);
  }

}