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

import static org.siglus.siglusapi.constant.FieldConstants.DOT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.Source;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class SiglusValidSourceDestinationService {

  private final ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;
  private final SupportedProgramsHelper supportedProgramsHelper;
  private final NodeRepository nodeRepository;
  private final FacilityRepository facilityRepository;
  private final AndroidHelper androidHelper;

  private Map<String, Collection<ValidSourceDestinationDto>> cacheKeyToValidSourceDestinationDto = new HashMap<>();

  public Collection<ValidSourceDestinationDto> findSourcesForOneProgram(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToSupervisoryFacilities = mapProgramIdToSupervisoryFacilities(facilityId,
        programIds);
    return findAllSources(programIdToSupervisoryFacilities, facilityId, programId);
  }

  public Collection<ValidSourceDestinationDto> findSourcesForAllPrograms(UUID facilityId) {
    Set<UUID> supportedProgramIds = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToSupervisoryFacilities = mapProgramIdToSupervisoryFacilities(facilityId,
        supportedProgramIds);
    return supportedProgramIds.stream()
        .map(programId -> findAllSources(programIdToSupervisoryFacilities, facilityId, programId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForOneProgram(UUID programId, UUID facilityId) {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    Map<UUID, List<UUID>> programIdToMemberFacilities = mapProgramIdToMemberFacilities(facilityId, programIds);
    return findAllDestinations(programIdToMemberFacilities, facilityId, programId);
  }

  public Collection<ValidSourceDestinationDto> findDestinationsForAllPrograms(UUID facilityId) {
    Set<UUID> supportedProgramIds = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    Map<UUID, List<UUID>> programIdToMemberFacilities = mapProgramIdToMemberFacilities(facilityId, supportedProgramIds);
    return supportedProgramIds.stream()
        .map(programId -> findAllDestinations(programIdToMemberFacilities, facilityId, programId))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  private Collection<ValidSourceDestinationDto> findAllSources(
      Map<UUID, List<UUID>> programIdToSupervisoryFacilities, UUID facilityId, UUID programId) {
    Collection<ValidSourceDestinationDto> facilitySources = Collections.emptyList();
    if (programIdToSupervisoryFacilities.size() > 0) {
      facilitySources = getSourceDestinationsByFacilityIds(programIdToSupervisoryFacilities.get(programId), programId);
    }
    boolean isAndroidContext = androidHelper.isAndroid();
    Collection<ValidSourceDestinationDto> commonSources = validSourceDestinationStockManagementService.getValidSources(
            programId, facilityId).stream()
        .filter(i -> !i.getNode().isRefDataFacility())
        .filter(i -> isAndroidContext
            || Arrays.stream(Source.values()).noneMatch(e -> e.getName().equals(i.getName())))
        .collect(Collectors.toList());
    Collection<ValidSourceDestinationDto> allSources = new ArrayList<>();
    allSources.addAll(facilitySources);
    allSources.addAll(commonSources);
    return allSources;
  }

  private Collection<ValidSourceDestinationDto> findAllDestinations(
      Map<UUID, List<UUID>> programIdToMemberFacilities, UUID facilityId, UUID programId) {
    Collection<ValidSourceDestinationDto> facilityDestinations = Collections.emptyList();
    if (programIdToMemberFacilities.size() > 0) {
      facilityDestinations = getSourceDestinationsByFacilityIds(programIdToMemberFacilities.get(programId), programId);
    }
    boolean isAndroidContext = androidHelper.isAndroid();
    Collection<ValidSourceDestinationDto> commonDestinations =
        validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId).stream()
            .filter(i -> !i.getNode().isRefDataFacility())
            .filter(i -> isAndroidContext || !Destination.UNPACK_KIT.getName().equals(i.getName()))
            .collect(Collectors.toList());
    Collection<ValidSourceDestinationDto> allDestinations = new ArrayList<>();
    allDestinations.addAll(facilityDestinations);
    allDestinations.addAll(commonDestinations);
    return allDestinations;
  }

  private Collection<ValidSourceDestinationDto> getSourceDestinationsByFacilityIds(List<UUID> facilityIds,
      UUID programId) {
    if (CollectionUtils.isEmpty(facilityIds)) {
      return Collections.emptyList();
    }
    String cacheKey = generateCacheKey(facilityIds, programId);
    if (cacheKeyToValidSourceDestinationDto.containsKey(cacheKey)) {
      return cacheKeyToValidSourceDestinationDto.get(cacheKey);
    }
    List<Node> nodes = nodeRepository.findByReferenceIdIn(facilityIds);
    List<Facility> facilities = facilityRepository.findByIdIn(facilityIds);
    Collection<ValidSourceDestinationDto> validSourceDestinationDtos = nodes.stream().map(node -> {
      ValidSourceDestinationDto dto = new ValidSourceDestinationDto();
      dto.setNode(node);
      dto.setProgramId(programId);
      String facilityName = facilities.stream().filter(i -> i.getId().equals(node.getReferenceId())).findFirst()
          .orElseThrow(() -> new NotFoundException("No such node facilityId: " + node.getReferenceId())).getName();
      dto.setName(facilityName);
      return dto;
    }).collect(Collectors.toList());
    cacheKeyToValidSourceDestinationDto.put(cacheKey, validSourceDestinationDtos);
    return validSourceDestinationDtos;
  }

  private String generateCacheKey(List<UUID> facilityIds, UUID programId) {
    StringBuilder builder = new StringBuilder();
    facilityIds.forEach(id -> builder.append(id).append(DOT));
    builder.append(programId.toString());
    return builder.toString();
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
