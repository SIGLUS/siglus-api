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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNullableByDefault;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class StockCardSearchService {

  private final SiglusAuthenticationHelper authHelper;

  private final StockManagementRepository stockManagementRepository;

  private final ProductMovementMapper mapper;

  private static final String UNPACK_KIT_TYPE = "UNPACK_KIT";

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
    return mapper.toAndroidResponse(period);
  }

  public FacilityProductMovementsResponse getProductMovementsByOrderables(Set<UUID> orderableIds) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    PeriodOfProductMovements period = stockManagementRepository.getAllProductMovements(facilityId, orderableIds);
    return mapper.toAndroidResponse(period);
  }

  public List<ProductMovementResponse> getProductMovementsByOrderablesAndFacility(
      Set<UUID> orderableIds, UUID facilityId, LocalDate since, LocalDate till) {
    PeriodOfProductMovements productMovements =
        stockManagementRepository.getAllProductMovements(facilityId, orderableIds, since, till);
    List<ProductMovementResponse> productMovementResponses = mapper.toResponses(productMovements);
    if (productMovementResponses.isEmpty()) {
      return null;
    }
    return simplifyStockMovementResponse(productMovementResponses);
  }

  private List<ProductMovementResponse> simplifyStockMovementResponse(
      List<ProductMovementResponse> productMovementResponses) {
    List<ProductMovementResponse> responses = productMovementResponses.stream()
        .map(productMovementResponse -> {
          List<SiglusStockMovementItemResponse> stockMovementItems =
              productMovementResponse.getStockMovementItems();
          productMovementResponse.setStockMovementItems(stockMovementItems.stream().map(items -> {
            items.setLotMovementItems(null);
            if (items.getType().equals(UNPACK_KIT_TYPE)) {
              items.setReason(UNPACK_KIT_TYPE);
            }
            return items;
          }).collect(Collectors.toList()));
          productMovementResponse.setLotsOnHand(null);
          return productMovementResponse;
        }).collect(Collectors.toList());
    return responses;
  }
}