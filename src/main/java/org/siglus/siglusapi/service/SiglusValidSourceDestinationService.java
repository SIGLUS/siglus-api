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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusValidSourceDestinationService {

  @Autowired
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  @Autowired
  private RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  @Autowired
  private SupportedProgramsHelper supportedVirtualProgramsHelper;

  public Collection<ValidSourceDestinationDto> findSources(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidSources(programId, facilityId);
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllProducts(UUID facilityId) {
    Set<UUID> supportedVirtualPrograms = supportedVirtualProgramsHelper
        .findUserSupportedPrograms();
    return supportedVirtualPrograms.stream()
        .map(supportedVirtualProgram -> findSources(supportedVirtualProgram, facilityId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findDestinations(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> mapGroupMembersDtoByProgramId = mapGroupMembersDtoByProgramId(
        facilityId, programIds);
    return filterFacilityByRequisitionGroup(mapGroupMembersDtoByProgramId.get(programId),
        findDestinationDtos(programId, facilityId));
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllProducts(UUID facilityId) {
    Set<UUID> supportedVirtualPrograms = supportedVirtualProgramsHelper
        .findUserSupportedPrograms();
    Map<UUID, List<UUID>> mapGroupMembersDtoByProgramId = mapGroupMembersDtoByProgramId(
        facilityId, supportedVirtualPrograms);
    return supportedVirtualPrograms.stream()
        .map(supportedVirtualProgram ->
            filterFacilityByRequisitionGroup(mapGroupMembersDtoByProgramId.get(supportedVirtualProgram),
                findDestinationDtos(supportedVirtualProgram, facilityId)))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private Collection<ValidSourceDestinationDto> findDestinationDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> filterFacilityByRequisitionGroup(
      List<UUID> facilityIds,
      Collection<ValidSourceDestinationDto> validSourceDestinationDtos) {
    Collection<ValidSourceDestinationDto> destinationDtosAfterFilter = new ArrayList<>();
    validSourceDestinationDtos.forEach(
        dto -> {
          if (!dto.getNode().isRefDataFacility() || facilityIds.contains(dto.getNode().getReferenceId())) {
            destinationDtosAfterFilter.add(dto);
          }
        }
    );
    return destinationDtosAfterFilter;
  }

  private Map<UUID, List<UUID>> mapGroupMembersDtoByProgramId(UUID facilityId,
      Set<UUID> supportedVirtualPrograms) {
    List<RequisitionGroupMembersDto> requisitionGroupMembersList = requisitionGroupMembersRepository
        .searchByFacilityIdAndProgramAndRequisitionGroup(facilityId, supportedVirtualPrograms);
    if (requisitionGroupMembersList.isEmpty()) {
      return Collections.emptyMap();
    }
    return requisitionGroupMembersList.stream()
        .collect(Collectors.groupingBy(RequisitionGroupMembersDto::getProgramId))
        .entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> getFacilityId(e.getValue())));
  }

  private List<UUID> getFacilityId(List<RequisitionGroupMembersDto> list) {
    return list.stream().map(RequisitionGroupMembersDto::getFacilityId).collect(Collectors.toList());
  }
}
