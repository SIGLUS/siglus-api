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

package org.siglus.siglusapi.service.android.context;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockCardCreateContextHolder {

  private static final ThreadLocal<StockCardCreateContext> HOLDER = new ThreadLocal<>();

  private final SiglusValidReasonAssignmentService reasonService;
  private final SiglusValidSourceDestinationService nodeService;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final StockManagementRepository stockManagementRepository;

  public static StockCardCreateContext getContext() {
    StockCardCreateContext context = HOLDER.get();
    if (context == null) {
      throw new IllegalStateException("Not init");
    }
    return context;
  }

  public static void clearContext() {
    HOLDER.remove();
  }

  public void initContext(FacilityDto facility, LocalDate earliest) {
    StocksOnHand stockOnHand = stockManagementRepository.getStockOnHand(facility.getId());
    earliest = stockOnHand.getTheEarliestDate(earliest);
    PeriodOfProductMovements allProductMovements = stockManagementRepository
        .getAllProductMovements(facility.getId(), earliest);
    UUID facilityTypeId = facility.getType().getId();
    Collection<ValidReasonAssignmentDto> reasons = reasonService.getAllReasons(facilityTypeId);
    Collection<ValidSourceDestinationDto> destinations = nodeService.findDestinationsForAllProducts(facility.getId());
    Collection<ValidSourceDestinationDto> sources = nodeService.findSourcesForAllProducts(facility.getId());

    List<OrderableDto> products = programsHelper.findHomeFacilitySupportedProgramIds().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(facility.getId(), program))
        .flatMap(Collection::stream)
        .collect(toList());
    StockCardCreateContext context = new StockCardCreateContext(facility, reasons, sources, destinations, products,
        allProductMovements);
    HOLDER.set(context);
  }

  private List<OrderableDto> getProgramProducts(UUID homeFacilityId, ProgramDto program) {
    return approvedProductDataService.getApprovedProducts(homeFacilityId, program.getId(), emptyList()).stream()
        .map(ApprovedProductDto::getOrderable)
        .collect(toList());
  }
}
