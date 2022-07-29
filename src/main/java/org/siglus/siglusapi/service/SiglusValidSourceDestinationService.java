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
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiglusValidSourceDestinationService {

  private static final String OUTROS = "Outros";
  private final ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;
  private final SupportedProgramsHelper supportedProgramsHelper;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final AndroidHelper androidHelper;

  public Collection<ValidSourceDestinationDto> findSources(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToSupervisoryFacilities = mapProgramIdToSupervisoryFacilities(facilityId,
        programIds);
    return filterFacilityByRequisitionGroup(programIdToSupervisoryFacilities.get(programId),
        findSourcesDtos(programId, facilityId), facilityId, true);
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToSupervisoryFacilities = mapProgramIdToSupervisoryFacilities(facilityId,
        supportedPrograms);
    return supportedPrograms.stream()
        .map(
            supportedProgram -> filterFacilityByRequisitionGroup(programIdToSupervisoryFacilities.get(supportedProgram),
                findSourcesDtos(supportedProgram, facilityId), facilityId, true))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findDestinations(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToMemberFacilities = mapProgramIdToMemberFacilities(facilityId, programIds);
    return filterFacilityByRequisitionGroup(programIdToMemberFacilities.get(programId),
        findDestinationDtos(programId, facilityId), facilityId, false);
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllProducts(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToMemberFacilities = mapProgramIdToMemberFacilities(facilityId, supportedPrograms);
    return supportedPrograms.stream()
        .map(supportedProgram ->
            filterFacilityByRequisitionGroup(programIdToMemberFacilities.get(supportedProgram),
                findDestinationDtos(supportedProgram, facilityId), facilityId, false))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private Collection<ValidSourceDestinationDto> findSourcesDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidSources(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> findDestinationDtos(UUID programId, UUID facilityId) {
    return validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId);
  }

  private Collection<ValidSourceDestinationDto> filterFacilityByRequisitionGroup(List<UUID> groupFacilityIds,
      Collection<ValidSourceDestinationDto> validSourceDestinationDtos, UUID facilityId, boolean isReceive) {
    Collection<ValidSourceDestinationDto> filterSourceDestinations = new ArrayList<>();
    validSourceDestinationDtos.forEach(
        dto -> {
          if (isShowFacilityNode(groupFacilityIds, dto) || isShowCommonNode(dto, isReceive)
              || isShowWarehouseNode(facilityId, isReceive)) {
            filterSourceDestinations.add(dto);
          }
        }
    );
    return filterSourceDestinations;
  }

  private boolean isShowFacilityNode(List<UUID> groupFacilityIds, ValidSourceDestinationDto dto) {
    return groupFacilityIds != null && groupFacilityIds.contains(dto.getNode().getReferenceId());
  }

  private boolean isShowCommonNode(ValidSourceDestinationDto dto, boolean isReceive) {
    boolean isCommonNode = !dto.getNode().isRefDataFacility();
    if (isReceive) {
      return isCommonNode && (androidHelper.isAndroid() || OUTROS.equals(dto.getName()));
    }
    return isCommonNode;
  }

  private boolean isShowWarehouseNode(UUID facilityId, boolean isReceive) {
    boolean isTopLevelFacility = FacilityTypeConstants.getTopLevelTypes()
        .contains(facilityReferenceDataService.findOne(facilityId).getType().getCode());
    return isReceive && isTopLevelFacility;
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
