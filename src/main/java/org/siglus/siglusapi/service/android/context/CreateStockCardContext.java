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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.OrderableDto;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.Source;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class CreateStockCardContext {

  private final Map<UUID, Map<String, UUID>> programToReasonNameToId;
  private final Map<UUID, Map<String, UUID>> programToSourceNameToId;
  private final Map<UUID, Map<String, UUID>> programToDestinationNameToId;
  private final List<OrderableDto> facilitySupportOrderables;

  public Optional<UUID> findReasonId(UUID programId, String reason) {
    Map<String, UUID> reasonNameToId = programToReasonNameToId.get(programId);
    if (reasonNameToId == null) {
      return Optional.empty();
    }
    String reasonName = AdjustmentReason.valueOf(reason).getName();
    return Optional.ofNullable(reasonNameToId.get(reasonName));
  }

  public Optional<UUID> findSourceId(UUID programId, String source) {
    Map<String, UUID> sourceNameToId = programToSourceNameToId.get(programId);
    if (sourceNameToId == null) {
      return Optional.empty();
    }
    String sourceName = Source.valueOf(source).getName();
    return Optional.ofNullable(sourceNameToId.get(sourceName));
  }

  public Optional<UUID> findDestinationId(UUID programId, String destination) {
    Map<String, UUID> destinationNameToId = programToDestinationNameToId.get(programId);
    if (destinationNameToId == null) {
      return Optional.empty();
    }
    String destinationName = Destination.valueOf(destination).getName();
    return Optional.ofNullable(destinationNameToId.get(destinationName));
  }

  public boolean isSupportByCurrentFacility(String productCode) {
    return facilitySupportOrderables.stream().anyMatch(orderable -> orderable.getProductCode().equals(productCode));
  }

}
