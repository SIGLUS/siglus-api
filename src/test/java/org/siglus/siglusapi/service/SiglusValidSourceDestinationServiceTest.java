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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.constant.FacilityTypeConstants;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusValidSourceDestinationServiceTest {

  @InjectMocks
  private SiglusValidSourceDestinationService siglusValidSourceDestinationService;

  @Mock
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  @Mock
  private RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  @Mock
  private SupportedProgramsHelper supportedVirtualProgramsHelper;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  private final UUID programId = UUID.randomUUID();

  private final UUID programId2 = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID facilityId2 = UUID.randomUUID();

  private final UUID facilityId3 = UUID.randomUUID();

  private final UUID facilityIdFalseNotContain = UUID.randomUUID();

  private final UUID facilityIdTrueNotContain = UUID.randomUUID();

  private final String destinationName = "destination";

  private final String destinationName2 = "destination2";

  private final String destinationName3 = "destination3";

  private final String destinationNameFalseNotContain = "destinationFalseNotContain";

  private final String destinationNameTrueNotContain = "destinationTrueNotContain";

  @Before
  public void prepare() {
    createDestinationData();
  }

  @Test
  public void shouldCallGetValidDestinationsWhenFindDestinations() {
    siglusValidSourceDestinationService.findDestinations(programId, facilityId);

    verify(validSourceDestinationStockManagementService)
        .getValidDestinations(programId, facilityId);
  }

  @Test
  public void shouldCallGetValidDestinationsMultipleTimesWhenFindDestinationsForAllProducts() {
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    siglusValidSourceDestinationService.findDestinationsForAllProducts(facilityId);

    verify(validSourceDestinationStockManagementService, times(2))
        .getValidDestinations(any(), any());
  }

  @Test
  public void shouldCallGetValidSourcesWhenFindSources() {
    siglusValidSourceDestinationService.findSources(programId, facilityId);

    verify(validSourceDestinationStockManagementService).getValidSources(programId, facilityId);
  }

  @Test
  public void shouldCallGetValidSourcesMultipleTimesWhenFindSourcesForAllProducts() {
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    siglusValidSourceDestinationService.findSourcesForAllProducts(facilityId);

    verify(validSourceDestinationStockManagementService, times(2))
        .getValidSources(any(), any());
  }

  @Test
  public void shouldGetDestinationWhenFacilityInRequisitionGroup() {
    // when
    List<String> destinationNameList = siglusValidSourceDestinationService.findDestinationsForAllProducts(facilityId)
        .stream()
        .map(ValidSourceDestinationDto::getName)
        .collect(Collectors.toList());

    // then
    assertEquals(true, destinationNameList.contains(destinationName));
    assertEquals(true, destinationNameList.contains(destinationName2));
    assertEquals(true, destinationNameList.contains(destinationName3));
    assertEquals(true, destinationNameList.contains(destinationNameFalseNotContain));
    assertEquals(false, destinationNameList.contains(destinationNameTrueNotContain));
  }

  @Test
  public void shouldNotGetDestinationInOrganizationWhenFacilityTypeInIssueFilterTypes() {
    // given
    when(facilityReferenceDataService.findOne(facilityId))
        .thenReturn(buildFacilityDtoByFaclityIdAndTypeCode(facilityId, FacilityTypeConstants.DPM));

    // when
    List<String> destinationNameList = siglusValidSourceDestinationService.findDestinationsForAllProducts(facilityId)
        .stream()
        .map(ValidSourceDestinationDto::getName)
        .collect(Collectors.toList());

    // then
    assertEquals(false, destinationNameList.contains(destinationName));
  }

  private void createDestinationData() {
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programId);
    programIds.add(programId2);
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms()).thenReturn(programIds);

    when(facilityReferenceDataService.findOne(facilityId))
        .thenReturn(buildFacilityDtoByFaclityIdAndTypeCode(facilityId, FacilityTypeConstants.CS));

    RequisitionGroupMembersDto reqProgramIdFacilityId = RequisitionGroupMembersDto.builder()
        .programId(programId).facilityId(facilityId).build();
    RequisitionGroupMembersDto reqProgramIdFacilityId2 = RequisitionGroupMembersDto.builder()
        .programId(programId).facilityId(facilityId2).build();
    RequisitionGroupMembersDto reqProgramId2FacilityId3 = RequisitionGroupMembersDto.builder()
        .programId(programId2).facilityId(facilityId3).build();
    when(requisitionGroupMembersRepository.searchByFacilityIdAndProgramAndRequisitionGroup(any(), any())).thenReturn(
        Arrays.asList(reqProgramIdFacilityId, reqProgramIdFacilityId2, reqProgramId2FacilityId3));

    ValidSourceDestinationDto desProgramNodeFalseContain = new ValidSourceDestinationDto();
    Node nodefalseContain = new Node();
    nodefalseContain.setReferenceId(facilityId);
    nodefalseContain.setRefDataFacility(false);
    desProgramNodeFalseContain.setNode(nodefalseContain);
    desProgramNodeFalseContain.setProgramId(programId);
    desProgramNodeFalseContain.setName(destinationName);

    ValidSourceDestinationDto desProgramNodeFalseNotContain = new ValidSourceDestinationDto();
    Node nodefalseNotContain = new Node();
    nodefalseNotContain.setReferenceId(facilityIdFalseNotContain);
    nodefalseNotContain.setRefDataFacility(false);
    desProgramNodeFalseNotContain.setNode(nodefalseNotContain);
    desProgramNodeFalseNotContain.setProgramId(programId);
    desProgramNodeFalseNotContain.setName(destinationNameFalseNotContain);

    ValidSourceDestinationDto desProgramNodeTrueContain = new ValidSourceDestinationDto();
    Node nodeTrueContain = new Node();
    nodeTrueContain.setReferenceId(facilityId2);
    nodeTrueContain.setRefDataFacility(true);
    desProgramNodeTrueContain.setNode(nodeTrueContain);
    desProgramNodeTrueContain.setProgramId(programId);
    desProgramNodeTrueContain.setName(destinationName2);

    ValidSourceDestinationDto desProgramNodeTrueNotContain = new ValidSourceDestinationDto();
    Node nodeTrueNotContain = new Node();
    nodeTrueNotContain.setReferenceId(facilityIdTrueNotContain);
    nodeTrueNotContain.setRefDataFacility(true);
    desProgramNodeTrueNotContain.setNode(nodeTrueNotContain);
    desProgramNodeTrueNotContain.setProgramId(programId);
    desProgramNodeTrueNotContain.setName(destinationNameTrueNotContain);

    ValidSourceDestinationDto desProgram2NodeTrueContain = new ValidSourceDestinationDto();
    Node node2TrueContain = new Node();
    node2TrueContain.setReferenceId(facilityId3);
    node2TrueContain.setRefDataFacility(true);
    desProgram2NodeTrueContain.setNode(node2TrueContain);
    desProgram2NodeTrueContain.setProgramId(programId2);
    desProgram2NodeTrueContain.setName(destinationName3);
    when(validSourceDestinationStockManagementService.getValidDestinations(programId, facilityId))
        .thenReturn(Arrays.asList(desProgramNodeFalseContain, desProgramNodeFalseNotContain, desProgramNodeTrueContain,
            desProgramNodeTrueNotContain));
    when(validSourceDestinationStockManagementService.getValidDestinations(programId2, facilityId))
        .thenReturn(Arrays.asList(desProgram2NodeTrueContain));
  }

  private FacilityDto buildFacilityDtoByFaclityIdAndTypeCode(UUID facilityId, String facilityTypeCode) {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setCode(facilityTypeCode);
    return FacilityDto.builder().id(facilityId).type(facilityTypeDto).build();
  }
}
