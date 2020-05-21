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

package org.openlmis.requisition.service.referencedata;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.DetailedRoleAssignmentDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.testutils.DetailedRoleAssignmentDtoDataBuilder;
import org.openlmis.requisition.testutils.DtoGenerator;

@RunWith(MockitoJUnitRunner.class)
public class UserRoleAssignmentsReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<DetailedRoleAssignmentDto> {

  private UserRoleAssignmentsReferenceDataService service;

  @Override
  protected DetailedRoleAssignmentDto generateInstance() {
    return new DetailedRoleAssignmentDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseReferenceDataService<DetailedRoleAssignmentDto> getService() {
    return new UserRoleAssignmentsReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (UserRoleAssignmentsReferenceDataService) prepareService();
  }

  private RightDto approveRequisitionRight = DtoGenerator.of(RightDto.class, 2).get(0);
  private RightDto convertToOrderRight = DtoGenerator.of(RightDto.class, 2).get(1);
  private RoleDto role = DtoGenerator.of(RoleDto.class);

  private UUID supervisoryNodeId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID userId = UUID.randomUUID();

  @Test
  public void shouldGetRoleAssignmentsForUser() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    DetailedRoleAssignmentDto dto = mockArrayResponseEntityAndGetDto();
    Collection<DetailedRoleAssignmentDto> result = service.getRoleAssignments(userId);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl() + userId + "/roleAssignments");
  }

  @Test
  public void shouldReturnTrueIfUserCanApproveRequisition() {
    mockSupervisionRoleAssignment(programId, supervisoryNodeId,
        approveRequisitionRight);

    assertTrue(service.hasSupervisionRight(approveRequisitionRight, userId,
        programId, supervisoryNodeId));
  }

  @Test
  public void shouldReturnFalseIfUserHasNoApproveRigthAssigned() {
    mockSupervisionRoleAssignment(programId, supervisoryNodeId, convertToOrderRight);

    assertFalse(service.hasSupervisionRight(approveRequisitionRight, userId,
        programId, supervisoryNodeId));
  }

  @Test
  public void shouldReturnFalseIfUserHasDifferentSupervisoryNodeAssignedToApproveRight() {
    mockSupervisionRoleAssignment(programId, UUID.randomUUID(),
        approveRequisitionRight);

    assertFalse(service.hasSupervisionRight(approveRequisitionRight, userId,
        programId, supervisoryNodeId));
  }

  @Test
  public void shouldReturnFalseIfUserHasDifferentProgramAssignedToApproveRight() {
    mockSupervisionRoleAssignment(UUID.randomUUID(), supervisoryNodeId,
        approveRequisitionRight);

    assertFalse(service.hasSupervisionRight(approveRequisitionRight, userId,
        programId, supervisoryNodeId));
  }

  @Test
  public void shouldReturnFalseIfUserOrRightIsNotGiven() {
    disableAuthCheck();
    assertFalse(service.hasSupervisionRight(null, userId, programId, supervisoryNodeId));
    assertFalse(service.hasSupervisionRight(approveRequisitionRight, null,
        programId, supervisoryNodeId));
  }

  private void mockSupervisionRoleAssignment(UUID program, UUID node, RightDto right) {
    DetailedRoleAssignmentDto detailedRoleAssignmentDto = mock(DetailedRoleAssignmentDto.class);
    when(detailedRoleAssignmentDto.getProgramId()).thenReturn(program);
    when(detailedRoleAssignmentDto.getSupervisoryNodeId()).thenReturn(node);
    when(detailedRoleAssignmentDto.getRole()).thenReturn(role);

    role.setRights(Sets.newHashSet(right));

    mockArrayResponseEntity(detailedRoleAssignmentDto);
  }
}
