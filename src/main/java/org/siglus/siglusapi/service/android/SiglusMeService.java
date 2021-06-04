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

import static java.util.Collections.emptyList;

import java.time.Instant;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.referencedata.MetaDataDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;
import org.siglus.siglusapi.dto.response.android.ProductResponse;
import org.siglus.siglusapi.dto.response.android.ProductSyncResponse;
import org.siglus.siglusapi.dto.response.android.ProgramResponse;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiglusMeService {

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusArchiveProductService siglusArchiveProductService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;

  public ProductSyncResponse getFacilityProducts(Instant lastSyncTime) {
    ProductSyncResponse syncResponse = new ProductSyncResponse();
    syncResponse.setLastSyncTime(System.currentTimeMillis());
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    List<OrderableDto> approvedProducts = programsHelper.findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .map(OrderablesAggregator::getOrderablesPage)
        .map(Slice::getContent)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    Map<UUID, OrderableDto> productMap = approvedProducts.stream()
        .collect(Collectors.toMap(OrderableDto::getId, Function.identity()));
    List<OrderableDto> filteredProducts = approvedProducts.stream()
        .filter(p -> filterByLastUpdated(p, lastSyncTime))
        .collect(Collectors.toList());
    syncResponse.setProducts(
        filteredProducts.stream()
            .map(orderable -> ProductResponse.fromOrderable(orderable, productMap))
            .collect(Collectors.toList()));
    return syncResponse;
  }

  private OrderablesAggregator getProgramProducts(UUID homeFacilityId, ProgramDto program) {
    OrderablesAggregator approvedProducts = approvedProductDataService
        .getApprovedProducts(homeFacilityId, program.getId(), emptyList());
    approvedProducts.getOrderablesPage()
        .forEach(orderable -> orderable.getExtraData().put("programCode", program.getCode()));
    return approvedProducts;
  }

  private boolean filterByLastUpdated(OrderableDto approvedProduct, Instant lastSyncTime) {
    if (lastSyncTime == null) {
      return true;
    }
    return Optional.of(approvedProduct)
        .map(OrderableDto::getMeta)
        .map(MetaDataDto::getLastUpdated)
        .map(ChronoZonedDateTime::toInstant)
        .map(lastUpdated -> !lastUpdated.isBefore(lastSyncTime))
        .orElse(true);
  }

  public FacilityResponse getFacility() {
    UserDto userDto = authHelper.getCurrentUser();
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
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    siglusArchiveProductService.archiveAllProducts(facilityId, productCodes);
  }
}
