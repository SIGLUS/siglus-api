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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateStockCardContextHolder {

  private static final ThreadLocal<CreateStockCardContext> HOLDER = new ThreadLocal<>();

  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final SiglusValidSourceDestinationService siglusValidSourceDestinationService;

  public void initContext(FacilityDto facility) {
    UUID facilityTypeId = facility.getType().getId();
    Map<UUID, Map<String, UUID>> programToReasonNameToId = validReasonAssignmentService
        .getValidReasonsForAllProducts(facilityTypeId, null, null)
        .stream()
        .collect(groupingBy(ValidReasonAssignmentDto::getProgramId,
            toMap(lineItem -> lineItem.getReason().getName(),
                validReasonAssignmentDto -> validReasonAssignmentDto.getReason().getId())));

    Map<UUID, Map<String, UUID>> programToDestinationNameToId = siglusValidSourceDestinationService
        .findDestinationsForAllProducts(facility.getId())
        .stream()
        .collect(groupingBy(ValidSourceDestinationDto::getProgramId,
            toMap(ValidSourceDestinationDto::getName,
                validSourceDestinationDto -> validSourceDestinationDto.getNode().getId())));

    Map<UUID, Map<String, UUID>> programToSourceNameToId = siglusValidSourceDestinationService
        .findSourcesForAllProducts(facility.getId())
        .stream()
        .collect(groupingBy(ValidSourceDestinationDto::getProgramId,
            toMap(ValidSourceDestinationDto::getName,
                validSourceDestinationDto -> validSourceDestinationDto.getNode().getId())));
    CreateStockCardContext context = new CreateStockCardContext(programToReasonNameToId, programToSourceNameToId,
        programToDestinationNameToId);
    HOLDER.set(context);
  }

  public static CreateStockCardContext getContext() {
    CreateStockCardContext context = HOLDER.get();
    if (context == null) {
      throw new IllegalStateException("Not init");
    }
    return context;
  }

  public static void clearContext() {
    HOLDER.remove();
  }
}
