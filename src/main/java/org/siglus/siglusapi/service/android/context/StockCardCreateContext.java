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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.Source;

public final class StockCardCreateContext {

  private final Map<NameBindToProgram, UUID> reasons;
  private final Map<NameBindToProgram, UUID> sources;
  private final Map<NameBindToProgram, UUID> destinations;
  private final Map<String, OrderableDto> approvedProducts;

  StockCardCreateContext(Collection<ValidReasonAssignmentDto> reasons, Collection<ValidSourceDestinationDto> sources,
      Collection<ValidSourceDestinationDto> destinations, Collection<OrderableDto> approvedProducts) {
    this.reasons = reasons.stream().collect(toMap(NameBindToProgram::of, v -> v.getReason().getId()));
    this.sources = sources.stream().collect(toMap(NameBindToProgram::of, v -> v.getNode().getId()));
    this.destinations = destinations.stream().collect(toMap(NameBindToProgram::of, v -> v.getNode().getId()));
    this.approvedProducts = approvedProducts.stream().collect(toMap(BasicOrderableDto::getProductCode, identity()));
  }

  public Optional<UUID> findReasonId(UUID programId, String reason) {
    String reasonName = AdjustmentReason.valueOf(reason).getName();
    UUID reasonId = reasons.get(NameBindToProgram.of(programId, reasonName));
    return Optional.ofNullable(reasonId);
  }

  public Optional<UUID> findSourceId(UUID programId, String source) {
    String sourceName = Source.valueOf(source).getName();
    UUID sourceId = sources.get(NameBindToProgram.of(programId, sourceName));
    return Optional.ofNullable(sourceId);
  }

  public Optional<UUID> findDestinationId(UUID programId, String destination) {
    String destinationName = Destination.valueOf(destination).getName();
    UUID destinationId = destinations.get(NameBindToProgram.of(programId, destinationName));
    return Optional.ofNullable(destinationId);
  }

  public boolean isApprovedByCurrentFacility(String productCode) {
    return approvedProducts.containsKey(productCode);
  }

  @Data
  @AllArgsConstructor(staticName = "of")
  private static class NameBindToProgram {

    private final UUID programId;
    private final String name;

    static NameBindToProgram of(ValidReasonAssignmentDto v) {
      return new NameBindToProgram(v.getProgramId(), v.getReason().getName());
    }

    static NameBindToProgram of(ValidSourceDestinationDto v) {
      return new NameBindToProgram(v.getProgramId(), v.getName());
    }

  }

}
