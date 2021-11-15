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
import lombok.RequiredArgsConstructor;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.constant.FacilityTypeConstants;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiglusValidSourceDestinationService {

  private final ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;
  private final SupportedProgramsHelper supportedProgramsHelper;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  public Collection<ValidSourceDestinationDto> findSources(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToSupervisoryFacilities(facilityId, programIds);
    return filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(programId),
        findSourcesDtos(programId, facilityId), isHighLevelFacility(facilityId));
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToSupervisoryFacilities(facilityId,
        supportedPrograms);
    boolean isHighLevelFacility = isHighLevelFacility(facilityId);
    return supportedPrograms.stream()
        .map(supportedProgram -> filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(supportedProgram),
            findSourcesDtos(supportedProgram, facilityId), isHighLevelFacility))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findDestinations(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToMemberFacilities(facilityId, programIds);
    return filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(programId),
        findDestinationDtos(programId, facilityId), isHighLevelFacility(facilityId));
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToGroupMembersDto = mapProgramIdToMemberFacilities(facilityId, supportedPrograms);
    boolean isHighLevelFacility = isHighLevelFacility(facilityId);
    return supportedPrograms.stream()
        .map(supportedProgram ->
            filterFacilityByRequisitionGroup(programIdToGroupMembersDto.get(supportedProgram),
                findDestinationDtos(supportedProgram, facilityId), isHighLevelFacility))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private Collection<ValidSourceDestinationDto> findSourcesDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidSources(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> findDestinationDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> filterFacilityByRequisitionGroup(List<UUID> facilityIds,
      Collection<ValidSourceDestinationDto> validSourceDestinationDtos, boolean isHighLevelFacility) {
    Collection<ValidSourceDestinationDto> filterSourceDestinations = new ArrayList<>();
    validSourceDestinationDtos.forEach(
        dto -> {
          boolean isFacilityNode = facilityIds != null && facilityIds.contains(dto.getNode().getReferenceId());
          boolean isCommonNode = !dto.getNode().isRefDataFacility();
          boolean isValidNode = isFacilityNode || isCommonNode;
          boolean isNotHighLevelFacilityWithCommonNode = !(isHighLevelFacility && isCommonNode);
          if (isValidNode && isNotHighLevelFacilityWithCommonNode) {
            filterSourceDestinations.add(dto);
          }
        }
    );
    return filterSourceDestinations;
  }

  private boolean isHighLevelFacility(UUID facilityId) {
    return FacilityTypeConstants.getAndroidOriginMovementTypes()
        .contains(facilityReferenceDataService.findOne(facilityId).getType().getCode());
  }

  private Map<UUID, List<UUID>> mapProgramIdToMemberFacilities(UUID facilityId, Set<UUID> supportedPrograms) {
    List<RequisitionGroupMembersDto> childrenFacilities = requisitionGroupMembersRepository
        .findChildrenFacilityByRequisitionGroup(facilityId, supportedPrograms);
    return convertListToMap(childrenFacilities);
  }

  private Map<UUID, List<UUID>> mapProgramIdToSupervisoryFacilities(UUID facilityId, Set<UUID> supportedPrograms) {
    List<RequisitionGroupMembersDto> parentFacilities = requisitionGroupMembersRepository
        .findParentFacilityByRequisitionGroup(facilityId, supportedPrograms);
    return convertListToMap(parentFacilities);
  }

  private Map<UUID, List<UUID>> convertListToMap(List<RequisitionGroupMembersDto> requisitionGroupMembersList) {
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
