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

package org.openlmis.fulfillment.service.stockmanagement;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.openlmis.fulfillment.service.request.RequestParameters;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDto;

public abstract class ValidSourceDestinationsStockManagementService
    extends BaseStockManagementService<ValidSourceDestinationDto> {

  @Override
  protected Class<ValidSourceDestinationDto> getResultClass() {
    return ValidSourceDestinationDto.class;
  }

  @Override
  protected Class<ValidSourceDestinationDto[]> getArrayResultClass() {
    return ValidSourceDestinationDto[].class;
  }

  /**
   * Try to find an instance of {@link ValidSourceDestinationDto} based on passed parameters.
   */
  public Optional<ValidSourceDestinationDto> search(UUID programId, UUID fromFacilityId,
      UUID toFacilityId) {
    Collection<ValidSourceDestinationDto> sources = search(programId, fromFacilityId);

    return sources
        .stream()
        .filter(elem -> elem.getNode().isRefDataFacility())
        .filter(elem -> Objects.equals(toFacilityId, elem.getNode().getReferenceId()))
        .findFirst();
  }

  private Collection<ValidSourceDestinationDto> search(UUID programId, UUID fromFacilityId) {
    return findAll("", RequestParameters.init()
        .set("programId", programId).set("facilityId", fromFacilityId));
  }
}
