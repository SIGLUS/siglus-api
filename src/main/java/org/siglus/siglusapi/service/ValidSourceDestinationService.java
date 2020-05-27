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

package org.siglus.siglusapi.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidSourceDestinationService {

  @Autowired
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  @Autowired
  private ProgramExtensionService programExtensionService;

  public Collection<ValidSourceDestinationDto> findDestinations(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId);
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllProducts(UUID facilityId) {
    Set<UUID> supportedVirtualPrograms = programExtensionService.findSupportedVirtualPrograms();
    return supportedVirtualPrograms.stream()
        .map(supportedVirtualProgram -> findDestinations(supportedVirtualProgram, facilityId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findSources(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidSources(programId, facilityId);
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllProducts(UUID facilityId) {
    Set<UUID> supportedVirtualPrograms = programExtensionService.findSupportedVirtualPrograms();
    return supportedVirtualPrograms.stream()
        .map(supportedVirtualProgram -> findSources(supportedVirtualProgram, facilityId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }
}
