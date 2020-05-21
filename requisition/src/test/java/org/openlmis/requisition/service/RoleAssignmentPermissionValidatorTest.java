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

package org.openlmis.requisition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;

public class RoleAssignmentPermissionValidatorTest
    extends BasePermissionValidatorTest {

  private static final String RIGHT_NAME = "RIGHT_NAME";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private RoleReferenceDataService roleReferenceDataService;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Mock
  private RequisitionGroupReferenceDataService requisitionGroupReferenceDataService;

  @InjectMocks
  private RoleAssignmentPermissionValidator validator;

  @Mock
  private Requisition requisition;

  private UserDto user = DtoGenerator.of(UserDto.class);
  private RightDto right = DtoGenerator.of(RightDto.class);
  private RoleDto role = DtoGenerator.of(RoleDto.class);
  private SupervisoryNodeDto supervisoryNode = DtoGenerator.of(SupervisoryNodeDto.class);
  private RequisitionGroupDto requisitionGroup = DtoGenerator.of(RequisitionGroupDto.class);
  private FacilityDto facility = DtoGenerator.of(FacilityDto.class);

  private UUID requisitionId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID facilityId = facility.getId();
  private UUID supervisoryNodeId = supervisoryNode.getId();

  @Before
  public void setUp() {
    super.setUp();

    supervisoryNode.setRequisitionGroup(new ObjectReferenceDto(requisitionGroup.getId()));
    requisitionGroup.setMemberFacilities(Sets.newHashSet(facility));

    when(requisition.getId()).thenReturn(requisitionId);
    when(requisition.getProgramId()).thenReturn(programId);
    when(requisition.getFacilityId()).thenReturn(facilityId);
    when(requisition.getSupplyingFacilityId()).thenReturn(facilityId);
    when(requisition.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(requisition.getSupervisoryNodeId()).thenReturn(supervisoryNodeId);

    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(authenticationHelper.getRight(RIGHT_NAME)).thenReturn(right);

    when(roleReferenceDataService.search(right.getId()))
        .thenReturn(Lists.newArrayList(role));

    when(supervisoryNodeReferenceDataService.findByIds(Sets.newHashSet(supervisoryNodeId)))
        .thenReturn(Lists.newArrayList(supervisoryNode));

    when(requisitionGroupReferenceDataService.findAll())
        .thenReturn(Lists.newArrayList(requisitionGroup));
  }

  @Test
  public void userShouldHavePermissionIfUserHasCorrectSupervisionRoleAssignment() {
    // given
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, supervisoryNodeId, null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldHavePermissionIfUserHasCorrectHomeFacilityRoleAssignment() {
    // given
    user.setHomeFacilityId(facilityId);
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, null, null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasIncorrectSupervisionRoleAssignment() {
    // given
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, UUID.randomUUID(), null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasIncorrectHomeFacilityRoleAssignment() {
    // given
    user.setHomeFacilityId(UUID.randomUUID());
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, null, null)));
    given(requisition.hasOriginalRequisitionId()).willReturn(true);

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasCorrectHomeFacilityRoleAssignmentForPartnerRnR() {
    // given
    user.setHomeFacilityId(UUID.randomUUID());
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, null, null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldNotHavePermissionIfThereAreNoRoles() {
    // given
    given(roleReferenceDataService.search(right.getId()))
        .willReturn(Collections.emptyList());

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldHavePermissionIfFacilityIsInRequisitionGroup() {
    // given
    given(requisition.getSupervisoryNodeId()).willReturn(null);
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, supervisoryNodeId, null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldNotHavePermissionIfFacilityIsNotInRequisitionGroup() {
    // given
    given(requisition.getSupervisoryNodeId()).willReturn(null);
    given(requisition.getFacilityId()).willReturn(UUID.randomUUID());
    user.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto(role.getId(),
        programId, supervisoryNodeId, null)));

    // when
    ValidationResult result = validator.hasPermission(getDetails());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Override
  PermissionValidationDetails getDetailsForGeneralPermission() {
    return getDetails();
  }

  @Override
  PermissionValidationDetails getDetailsForSupervisionPermission() {
    return getDetails();
  }

  @Override
  PermissionValidationDetails getDetailsForFulfillmentPermission() {
    return getDetails();
  }

  private PermissionValidationDetails getDetails() {
    return new RoleAssignmentPermissionValidationDetails(RIGHT_NAME, requisition);
  }
}
