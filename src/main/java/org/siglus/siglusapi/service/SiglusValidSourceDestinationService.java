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
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.constant.FacilityTypeConstants;
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
  private SupportedProgramsHelper supportedProgramsHelper;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  public Collection<ValidSourceDestinationDto> findSources(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidSources(programId, facilityId);
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findUserSupportedPrograms();
    return supportedPrograms.stream()
        .map(supportedProgram -> findSources(supportedProgram, facilityId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findDestinations(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToGroupMembersDto(facilityId, programIds);
    return filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(programId),
        findDestinationDtos(programId, facilityId), isFilterFacility(facilityId));
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findUserSupportedPrograms();
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToGroupMembersDto(facilityId, supportedPrograms);
    boolean isFilterFacility = isFilterFacility(facilityId);
    return supportedPrograms.stream()
        .map(supportedProgram ->
            filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(supportedProgram),
                findDestinationDtos(supportedProgram, facilityId), isFilterFacility))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private Collection<ValidSourceDestinationDto> findDestinationDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> filterFacilityByRequisitionGroup(List<UUID> facilityIds,
      Collection<ValidSourceDestinationDto> validSourceDestinationDtos, boolean isFilterFacility) {
    Collection<ValidSourceDestinationDto> destinationDtosAfterFilter = new ArrayList<>();
    validSourceDestinationDtos.forEach(
        dto -> {
          boolean isCommonDestination = !dto.getNode().isRefDataFacility();
          boolean isDestinationFromFacilityName =
              facilityIds != null && facilityIds.contains(dto.getNode().getReferenceId());
          boolean isIgnoreDestinationFromFacilityType = isFilterFacility && isCommonDestination;
          if ((isCommonDestination || isDestinationFromFacilityName) && !isIgnoreDestinationFromFacilityType) {
            destinationDtosAfterFilter.add(dto);
          }
        }
    );
    return destinationDtosAfterFilter;
  }

  private boolean isFilterFacility(UUID facilityId) {
    return FacilityTypeConstants.getIssueFilterFacilityTypes()
        .contains(facilityReferenceDataService.findOne(facilityId).getType().getCode());
  }

  private Map<UUID, List<UUID>> mapProgramIdToGroupMembersDto(UUID facilityId,
      Set<UUID> supportedPrograms) {
    List<RequisitionGroupMembersDto> requisitionGroupMembersList = requisitionGroupMembersRepository
        .searchByFacilityIdAndProgramAndRequisitionGroup(facilityId, supportedPrograms);
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
